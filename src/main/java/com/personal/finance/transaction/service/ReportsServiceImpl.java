package com.personal.finance.transaction.service;

import com.personal.finance.common.exception.ValidationException;
import com.personal.finance.transaction.dto.response.ReportsSummaryResponse;
import com.personal.finance.transaction.entity.Category;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.repository.CategoryRepository;
import com.personal.finance.transaction.repository.TransactionRepository;
import com.personal.finance.transaction.repository.projection.AccountBalanceAggregate;
import com.personal.finance.transaction.repository.projection.CategorySpendAggregate;
import com.personal.finance.transaction.repository.projection.MerchantSpendAggregate;
import com.personal.finance.transaction.repository.projection.MonthlyTypeAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements {@link ReportsService}. See REQ-reports-summary.md for the
 * authoritative spec; this file is the runtime translation of §3.
 *
 * <p>One {@code @Transactional(readOnly = true)} wraps every aggregate so
 * the queries observe a consistent snapshot. v1 issues queries
 * sequentially — measurement before adding {@code CompletableFuture}
 * parallelism (spec §5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsServiceImpl implements ReportsService {

    static final String RANGE_MONTH = "month";
    static final String RANGE_YEAR = "year";
    private static final int TREND_BUCKETS = 12;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportsSummaryResponse getSummary(String userId,
                                             String range,
                                             LocalDate asOf,
                                             Collection<UUID> accountIds) {
        String normalisedRange = validateRange(range);
        LocalDate effectiveAsOf = validateAsOf(asOf);
        boolean hasAccountFilter = accountIds != null;
        boolean emptyFilter = hasAccountFilter && accountIds.isEmpty();

        if (emptyFilter) {
            return emptySummary(normalisedRange, effectiveAsOf);
        }

        List<String> currencies = hasAccountFilter
                ? transactionRepository.findDistinctCurrenciesForAccounts(userId, effectiveAsOf, accountIds)
                : transactionRepository.findDistinctCurrencies(userId, effectiveAsOf);

        String topLevelCurrency = currencies.size() == 1 ? currencies.get(0) : null;
        boolean mixed = currencies.size() > 1;
        boolean empty = currencies.isEmpty();

        LocalDate windowStart = startOfRange(normalisedRange, effectiveAsOf);
        LocalDate trendStart = startOfTrend(effectiveAsOf);
        LocalDate trendEnd = endOfMonth(effectiveAsOf);

        ReportsSummaryResponse.NetWorth netWorth = empty
                ? zeroNetWorth()
                : computeNetWorth(userId, accountIds, hasAccountFilter, normalisedRange, effectiveAsOf, mixed);

        List<ReportsSummaryResponse.CategorySpend> spendByCategory = empty
                ? List.of()
                : computeSpendByCategory(userId, accountIds, hasAccountFilter, windowStart, effectiveAsOf);

        TrendArrays trend = empty
                ? TrendArrays.empty(trendStart)
                : computeTrend(userId, accountIds, hasAccountFilter, trendStart, trendEnd);

        List<ReportsSummaryResponse.MerchantSpend> topMerchants = empty
                ? List.of()
                : computeTopMerchants(userId, accountIds, hasAccountFilter, windowStart, effectiveAsOf);

        return ReportsSummaryResponse.builder()
                .range(normalisedRange)
                .asOf(effectiveAsOf)
                .currency(topLevelCurrency)
                .netWorth(netWorth)
                .spendByCategory(spendByCategory)
                .incomeByMonth(trend.income)
                .spendByMonth(trend.spend)
                .topMerchants(topMerchants)
                .build();
    }

    // ── validation ───────────────────────────────────────────────────────

    private String validateRange(String range) {
        if (range == null || !(RANGE_MONTH.equals(range) || RANGE_YEAR.equals(range))) {
            throw new ValidationException("range", "must be 'month' or 'year'");
        }
        return range;
    }

    private LocalDate validateAsOf(LocalDate asOf) {
        LocalDate today = LocalDate.now();
        if (asOf == null) {
            return today;
        }
        if (asOf.isAfter(today)) {
            throw new ValidationException("asOf", "must not be in the future");
        }
        return asOf;
    }

    // ── windows ──────────────────────────────────────────────────────────

    private LocalDate startOfRange(String range, LocalDate asOf) {
        return RANGE_YEAR.equals(range)
                ? asOf.withDayOfYear(1)
                : asOf.withDayOfMonth(1);
    }

    /** First day of the month, 11 months before asOf's month — start of the 12-bucket trend. */
    private LocalDate startOfTrend(LocalDate asOf) {
        return asOf.withDayOfMonth(1).minusMonths(TREND_BUCKETS - 1L);
    }

    private LocalDate endOfMonth(LocalDate asOf) {
        return asOf.withDayOfMonth(asOf.lengthOfMonth());
    }

    private LocalDate rewoundAsOf(String range, LocalDate asOf) {
        return RANGE_YEAR.equals(range) ? asOf.minusYears(1) : asOf.minusMonths(1);
    }

    // ── netWorth ─────────────────────────────────────────────────────────

    private ReportsSummaryResponse.NetWorth computeNetWorth(String userId,
                                                            Collection<UUID> accountIds,
                                                            boolean hasAccountFilter,
                                                            String range,
                                                            LocalDate asOf,
                                                            boolean mixedCurrency) {
        if (mixedCurrency) {
            return ReportsSummaryResponse.NetWorth.builder()
                    .current(null).previous(null).delta(null).build();
        }

        BigDecimal current = aggregateBalanceAt(userId, accountIds, hasAccountFilter, asOf);
        BigDecimal previous = aggregateBalanceAt(userId, accountIds, hasAccountFilter, rewoundAsOf(range, asOf));
        BigDecimal delta = current.subtract(previous);

        return ReportsSummaryResponse.NetWorth.builder()
                .current(current).previous(previous).delta(delta).build();
    }

    /**
     * Reuse {@code aggregateBalances} / {@code aggregateBalancesForAccounts}.
     * Those group by {@code (accountId, currency)} — when the user is
     * single-currency (the only path that reaches here) every row shares one
     * currency and we sum the {@code credit - debit} differences in memory.
     */
    private BigDecimal aggregateBalanceAt(String userId,
                                          Collection<UUID> accountIds,
                                          boolean hasAccountFilter,
                                          LocalDate asOf) {
        List<AccountBalanceAggregate> rows = hasAccountFilter
                ? transactionRepository.aggregateBalancesForAccounts(
                        userId, asOf, accountIds, EntryType.CREDIT, EntryType.DEBIT)
                : transactionRepository.aggregateBalances(
                        userId, asOf, EntryType.CREDIT, EntryType.DEBIT);

        BigDecimal sum = BigDecimal.ZERO;
        for (AccountBalanceAggregate row : rows) {
            sum = sum.add(row.getTotalCredit().subtract(row.getTotalDebit()));
        }
        return sum;
    }

    private ReportsSummaryResponse.NetWorth zeroNetWorth() {
        return ReportsSummaryResponse.NetWorth.builder()
                .current(BigDecimal.ZERO)
                .previous(BigDecimal.ZERO)
                .delta(BigDecimal.ZERO)
                .build();
    }

    // ── spendByCategory ──────────────────────────────────────────────────

    private List<ReportsSummaryResponse.CategorySpend> computeSpendByCategory(
            String userId,
            Collection<UUID> accountIds,
            boolean hasAccountFilter,
            LocalDate from,
            LocalDate to) {
        List<CategorySpendAggregate> rows = hasAccountFilter
                ? transactionRepository.aggregateSpendByCategoryForAccounts(
                        userId, from, to, accountIds, EntryType.DEBIT)
                : transactionRepository.aggregateSpendByCategory(userId, from, to, EntryType.DEBIT);

        if (rows.isEmpty()) return List.of();

        // One fetch of all active user categories — handful of rows per user,
        // cheaper than N point-lookups and keeps soft-deleted ones absent so
        // they collapse to name=null per spec §3 table.
        Map<UUID, String> nameById = new HashMap<>();
        for (Category cat : categoryRepository.findActiveByUserId(userId)) {
            nameById.put(cat.getCategoryId(), cat.getName());
        }

        List<ReportsSummaryResponse.CategorySpend> out = new ArrayList<>(rows.size());
        for (CategorySpendAggregate row : rows) {
            UUID cid = row.getCategoryId();
            String name = cid == null ? null : nameById.get(cid);
            out.add(ReportsSummaryResponse.CategorySpend.builder()
                    .categoryId(cid)
                    .name(name)
                    .total(row.getTotal())
                    .txnCount(row.getTxnCount())
                    .build());
        }
        out.sort(CATEGORY_SPEND_ORDER);
        return out;
    }

    /** total DESC, txnCount DESC, name ASC (nulls last). */
    private static final Comparator<ReportsSummaryResponse.CategorySpend> CATEGORY_SPEND_ORDER =
            Comparator.comparing(ReportsSummaryResponse.CategorySpend::getTotal, Comparator.reverseOrder())
                    .thenComparing(ReportsSummaryResponse.CategorySpend::getTxnCount, Comparator.reverseOrder())
                    .thenComparing(ReportsSummaryResponse.CategorySpend::getName,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    // ── monthly trend ────────────────────────────────────────────────────

    private record TrendArrays(List<ReportsSummaryResponse.MonthlyTotal> income,
                               List<ReportsSummaryResponse.MonthlyTotal> spend) {
        static TrendArrays empty(LocalDate trendStart) {
            return new TrendArrays(zeroPaddedTrend(trendStart), zeroPaddedTrend(trendStart));
        }
    }

    private TrendArrays computeTrend(String userId,
                                     Collection<UUID> accountIds,
                                     boolean hasAccountFilter,
                                     LocalDate from,
                                     LocalDate to) {
        List<MonthlyTypeAggregate> rows = hasAccountFilter
                ? transactionRepository.aggregateMonthlyByTypeForAccounts(userId, from, to, accountIds)
                : transactionRepository.aggregateMonthlyByType(userId, from, to);

        Map<YearMonth, BigDecimal> creditByMonth = new HashMap<>();
        Map<YearMonth, BigDecimal> debitByMonth = new HashMap<>();
        for (MonthlyTypeAggregate row : rows) {
            YearMonth ym = YearMonth.of(row.getYear(), row.getMonth());
            if (row.getEntryType() == EntryType.CREDIT) {
                creditByMonth.merge(ym, row.getTotal(), BigDecimal::add);
            } else if (row.getEntryType() == EntryType.DEBIT) {
                debitByMonth.merge(ym, row.getTotal(), BigDecimal::add);
            }
        }

        YearMonth start = YearMonth.from(from);
        List<ReportsSummaryResponse.MonthlyTotal> income = new ArrayList<>(TREND_BUCKETS);
        List<ReportsSummaryResponse.MonthlyTotal> spend = new ArrayList<>(TREND_BUCKETS);
        for (int i = 0; i < TREND_BUCKETS; i++) {
            YearMonth ym = start.plusMonths(i);
            income.add(monthlyTotal(ym, creditByMonth.getOrDefault(ym, BigDecimal.ZERO)));
            spend.add(monthlyTotal(ym, debitByMonth.getOrDefault(ym, BigDecimal.ZERO)));
        }
        return new TrendArrays(income, spend);
    }

    private static List<ReportsSummaryResponse.MonthlyTotal> zeroPaddedTrend(LocalDate from) {
        YearMonth start = YearMonth.from(from);
        List<ReportsSummaryResponse.MonthlyTotal> out = new ArrayList<>(TREND_BUCKETS);
        for (int i = 0; i < TREND_BUCKETS; i++) {
            out.add(monthlyTotal(start.plusMonths(i), BigDecimal.ZERO));
        }
        return Collections.unmodifiableList(out);
    }

    private static ReportsSummaryResponse.MonthlyTotal monthlyTotal(YearMonth ym, BigDecimal total) {
        return ReportsSummaryResponse.MonthlyTotal.builder()
                .month(ym)
                .total(total == null ? BigDecimal.ZERO : total)
                .build();
    }

    // ── topMerchants ─────────────────────────────────────────────────────

    private List<ReportsSummaryResponse.MerchantSpend> computeTopMerchants(
            String userId,
            Collection<UUID> accountIds,
            boolean hasAccountFilter,
            LocalDate from,
            LocalDate to) {
        // Native query takes the EntryType as a string because Hibernate
        // can't coerce the enum positionally for native SQL bindings.
        String debit = EntryType.DEBIT.name();
        List<MerchantSpendAggregate> rows = hasAccountFilter
                ? transactionRepository.findTopMerchantsForAccounts(userId, from, to, accountIds, debit)
                : transactionRepository.findTopMerchants(userId, from, to, debit);

        List<ReportsSummaryResponse.MerchantSpend> out = new ArrayList<>(rows.size());
        for (MerchantSpendAggregate row : rows) {
            out.add(ReportsSummaryResponse.MerchantSpend.builder()
                    .description(row.getDescription())
                    .total(row.getTotal())
                    .txnCount(row.getTxnCount())
                    .build());
        }
        return out;
    }

    // ── empty user fast path ─────────────────────────────────────────────

    /**
     * {@code accountIds=[]} short-circuit per spec §2.1: no DB hit, every
     * aggregate is the zero/empty shape. Keeps the Dashboard's "no accounts
     * selected" toggle cheap.
     */
    private ReportsSummaryResponse emptySummary(String range, LocalDate asOf) {
        LocalDate trendStart = startOfTrend(asOf);
        return ReportsSummaryResponse.builder()
                .range(range)
                .asOf(asOf)
                .currency(null)
                .netWorth(zeroNetWorth())
                .spendByCategory(List.of())
                .incomeByMonth(zeroPaddedTrend(trendStart))
                .spendByMonth(zeroPaddedTrend(trendStart))
                .topMerchants(List.of())
                .build();
    }
}
