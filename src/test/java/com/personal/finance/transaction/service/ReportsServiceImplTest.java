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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsServiceImplTest {

    private static final String USER_ID = "user-789";
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 4);

    @Mock TransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;

    ReportsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportsServiceImpl(transactionRepository, categoryRepository);
    }

    // ── range / asOf validation ──────────────────────────────────────────

    @Test
    void summary_givenMissingRange_throws400() {
        assertThatThrownBy(() -> service.getSummary(USER_ID, null, FIXED_TODAY, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("range"));
        verify(transactionRepository, never()).findDistinctCurrencies(anyString(), any());
    }

    @Test
    void summary_givenInvalidRange_throws400() {
        assertThatThrownBy(() -> service.getSummary(USER_ID, "decade", FIXED_TODAY, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void summary_givenAsOfInFuture_throws400() {
        LocalDate future = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> service.getSummary(USER_ID, "month", future, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("asOf"));
    }

    @Test
    void summary_givenNoAsOf_defaultsToToday() {
        when(transactionRepository.findDistinctCurrencies(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", null, null);

        assertThat(resp.getAsOf()).isEqualTo(LocalDate.now());
    }

    // ── range=month vs range=year window ─────────────────────────────────

    @Test
    void summary_givenRangeMonth_returnsCurrentMonthSpendByCategory() {
        UUID groceries = UUID.randomUUID();
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalances(eq(USER_ID), any(), eq(EntryType.CREDIT), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategory(
                eq(USER_ID),
                eq(LocalDate.of(2026, 6, 1)),
                eq(FIXED_TODAY),
                eq(EntryType.DEBIT)))
                .thenReturn(List.of(spend(groceries, "12.50", 2)));
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT"))).thenReturn(List.of());
        when(categoryRepository.findActiveByUserId(USER_ID))
                .thenReturn(List.of(category(groceries, "Groceries")));

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getRange()).isEqualTo("month");
        assertThat(resp.getCurrency()).isEqualTo("HKD");
        assertThat(resp.getSpendByCategory()).hasSize(1);
        assertThat(resp.getSpendByCategory().get(0).getName()).isEqualTo("Groceries");
        verify(transactionRepository).aggregateSpendByCategory(
                USER_ID, LocalDate.of(2026, 6, 1), FIXED_TODAY, EntryType.DEBIT);
    }

    @Test
    void summary_givenRangeYear_usesYTDWindow() {
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalances(eq(USER_ID), any(), any(), any())).thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategory(
                eq(USER_ID), eq(LocalDate.of(2026, 1, 1)), eq(FIXED_TODAY), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), eq(LocalDate.of(2026, 1, 1)), eq(FIXED_TODAY), eq("DEBIT")))
                .thenReturn(List.of());

        service.getSummary(USER_ID, "year", FIXED_TODAY, null);

        verify(transactionRepository).aggregateSpendByCategory(
                USER_ID, LocalDate.of(2026, 1, 1), FIXED_TODAY, EntryType.DEBIT);
        verify(transactionRepository).findTopMerchants(
                USER_ID, LocalDate.of(2026, 1, 1), FIXED_TODAY, "DEBIT");
    }

    // ── accountIds scoping ───────────────────────────────────────────────

    @Test
    void summary_givenAccountIds_scopesAllAggregates() {
        List<UUID> accountIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(transactionRepository.findDistinctCurrenciesForAccounts(USER_ID, FIXED_TODAY, accountIds))
                .thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalancesForAccounts(
                eq(USER_ID), any(), eq(accountIds), eq(EntryType.CREDIT), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategoryForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateMonthlyByTypeForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds)))
                .thenReturn(List.of());
        when(transactionRepository.findTopMerchantsForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds), eq("DEBIT")))
                .thenReturn(List.of());

        service.getSummary(USER_ID, "month", FIXED_TODAY, accountIds);

        verify(transactionRepository).findDistinctCurrenciesForAccounts(USER_ID, FIXED_TODAY, accountIds);
        // netWorth issues two reads — current (asOf) and previous (asOf − 1 month).
        verify(transactionRepository, times(2)).aggregateBalancesForAccounts(
                eq(USER_ID), any(), eq(accountIds), eq(EntryType.CREDIT), eq(EntryType.DEBIT));
        verify(transactionRepository).aggregateSpendByCategoryForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds), eq(EntryType.DEBIT));
        verify(transactionRepository).aggregateMonthlyByTypeForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds));
        verify(transactionRepository).findTopMerchantsForAccounts(
                eq(USER_ID), any(), any(), eq(accountIds), eq("DEBIT"));
        verify(transactionRepository, never()).findDistinctCurrencies(anyString(), any());
        verify(transactionRepository, never()).aggregateBalances(anyString(), any(), any(), any());
    }

    @Test
    void summary_givenEmptyAccountIds_shortCircuits_returnsZeroNetWorth_and12EmptyMonths() {
        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, List.of());

        assertThat(resp.getCurrency()).isNull();
        assertThat(resp.getNetWorth().getCurrent()).isEqualByComparingTo("0.00");
        assertThat(resp.getNetWorth().getPrevious()).isEqualByComparingTo("0.00");
        assertThat(resp.getNetWorth().getDelta()).isEqualByComparingTo("0.00");
        assertThat(resp.getSpendByCategory()).isEmpty();
        assertThat(resp.getIncomeByMonth()).hasSize(12);
        assertThat(resp.getSpendByMonth()).hasSize(12);
        assertThat(resp.getTopMerchants()).isEmpty();
        verify(transactionRepository, never()).findDistinctCurrencies(anyString(), any());
        verify(transactionRepository, never()).findDistinctCurrenciesForAccounts(anyString(), any(), any());
    }

    // ── currency modes ───────────────────────────────────────────────────

    @Test
    void summary_givenMixedCurrencies_nullsTopLevelCurrencyAndNetWorth_butKeepsAggregates() {
        UUID groceries = UUID.randomUUID();
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY))
                .thenReturn(List.of("HKD", "USD"));
        when(transactionRepository.aggregateSpendByCategory(eq(USER_ID), any(), any(), eq(EntryType.DEBIT)))
                .thenReturn(List.of(spend(groceries, "100.00", 1)));
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT"))).thenReturn(List.of());
        when(categoryRepository.findActiveByUserId(USER_ID))
                .thenReturn(List.of(category(groceries, "Groceries")));

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getCurrency()).isNull();
        assertThat(resp.getNetWorth().getCurrent()).isNull();
        assertThat(resp.getNetWorth().getPrevious()).isNull();
        assertThat(resp.getNetWorth().getDelta()).isNull();
        assertThat(resp.getSpendByCategory()).hasSize(1);
        assertThat(resp.getSpendByCategory().get(0).getTotal()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).aggregateBalances(anyString(), any(), any(), any());
    }

    @Test
    void summary_givenZeroTxnsInScope_returnsNullCurrency_zeroNetWorth_andEmpty12BucketTrend() {
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of());

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getCurrency()).isNull();
        assertThat(resp.getNetWorth().getCurrent()).isEqualByComparingTo("0.00");
        assertThat(resp.getNetWorth().getPrevious()).isEqualByComparingTo("0.00");
        assertThat(resp.getNetWorth().getDelta()).isEqualByComparingTo("0.00");
        assertThat(resp.getIncomeByMonth()).hasSize(12);
        assertThat(resp.getSpendByMonth()).hasSize(12);
        assertThat(resp.getIncomeByMonth().get(11).getMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(resp.getIncomeByMonth().get(0).getMonth()).isEqualTo(YearMonth.of(2025, 7));
        assertThat(resp.getSpendByCategory()).isEmpty();
        assertThat(resp.getTopMerchants()).isEmpty();
        verify(transactionRepository, never()).aggregateBalances(anyString(), any(), any(), any());
        verify(transactionRepository, never()).aggregateSpendByCategory(anyString(), any(), any(), any());
    }

    // ── netWorth: previous rewind + delta ────────────────────────────────

    @Test
    void summary_givenSingleCurrency_computesNetWorthDelta_andRewindsByOneMonth() {
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        // current: credit 200, debit 50 => net 150
        when(transactionRepository.aggregateBalances(USER_ID, FIXED_TODAY, EntryType.CREDIT, EntryType.DEBIT))
                .thenReturn(List.of(balance("200.00", "50.00")));
        // previous (1 month rewind): credit 100, debit 30 => net 70
        when(transactionRepository.aggregateBalances(
                USER_ID, FIXED_TODAY.minusMonths(1), EntryType.CREDIT, EntryType.DEBIT))
                .thenReturn(List.of(balance("100.00", "30.00")));
        when(transactionRepository.aggregateSpendByCategory(eq(USER_ID), any(), any(), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT"))).thenReturn(List.of());

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getNetWorth().getCurrent()).isEqualByComparingTo("150.00");
        assertThat(resp.getNetWorth().getPrevious()).isEqualByComparingTo("70.00");
        assertThat(resp.getNetWorth().getDelta()).isEqualByComparingTo("80.00");
    }

    // ── spendByCategory ordering + soft-deleted category ────────────────

    @Test
    void summary_spendByCategory_sortsTotalDescAndSurfacesSoftDeletedAsNullName() {
        UUID groceries = UUID.randomUUID();
        UUID utilities = UUID.randomUUID();
        UUID softDeleted = UUID.randomUUID();
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalances(eq(USER_ID), any(), any(), any())).thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategory(eq(USER_ID), any(), any(), eq(EntryType.DEBIT)))
                .thenReturn(List.of(
                        spend(utilities,   "100.00", 1),
                        spend(softDeleted, "500.00", 1),
                        spend(groceries,   "300.00", 2),
                        spend(null,        "200.00", 3) // uncategorised
                ));
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT"))).thenReturn(List.of());
        // softDeleted absent from the active list — surfaces as name=null
        when(categoryRepository.findActiveByUserId(USER_ID)).thenReturn(List.of(
                category(groceries, "Groceries"),
                category(utilities, "Utilities")
        ));

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        List<ReportsSummaryResponse.CategorySpend> rows = resp.getSpendByCategory();
        assertThat(rows).extracting("total")
                .containsExactly(
                        new BigDecimal("500.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("100.00"));
        // soft-deleted category keeps its row but with null name
        assertThat(rows.get(0).getCategoryId()).isEqualTo(softDeleted);
        assertThat(rows.get(0).getName()).isNull();
        // uncategorised row sits where its total places it (not pinned)
        assertThat(rows.get(2).getCategoryId()).isNull();
        assertThat(rows.get(2).getName()).isNull();
    }

    // ── topMerchants pass-through ────────────────────────────────────────

    @Test
    void summary_topMerchants_returnsRepositoryRowsInOrder() {
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalances(eq(USER_ID), any(), any(), any())).thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategory(eq(USER_ID), any(), any(), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT")))
                .thenReturn(List.of(
                        merchant("ParknShop", "1450.20", 12),
                        merchant("Wellcome",   "800.00",  5)
                ));

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getTopMerchants()).hasSize(2);
        assertThat(resp.getTopMerchants().get(0).getDescription()).isEqualTo("ParknShop");
        assertThat(resp.getTopMerchants().get(0).getTotal()).isEqualByComparingTo("1450.20");
        assertThat(resp.getTopMerchants().get(0).getTxnCount()).isEqualTo(12);
    }

    // ── monthly trend zero-padding + month-bucket pivot ─────────────────

    @Test
    void summary_monthlyTrend_pivotsRowsAndZeroPadsMissingMonths() {
        when(transactionRepository.findDistinctCurrencies(USER_ID, FIXED_TODAY)).thenReturn(List.of("HKD"));
        when(transactionRepository.aggregateBalances(eq(USER_ID), any(), any(), any())).thenReturn(List.of());
        when(transactionRepository.aggregateSpendByCategory(eq(USER_ID), any(), any(), eq(EntryType.DEBIT)))
                .thenReturn(List.of());
        when(transactionRepository.findTopMerchants(eq(USER_ID), any(), any(), eq("DEBIT"))).thenReturn(List.of());
        when(transactionRepository.aggregateMonthlyByType(eq(USER_ID), any(), any())).thenReturn(List.of(
                monthly(2026, 6, EntryType.CREDIT, "56800.00"),
                monthly(2026, 6, EntryType.DEBIT,  "12000.00"),
                monthly(2025, 8, EntryType.CREDIT, "5000.00")
        ));

        ReportsSummaryResponse resp = service.getSummary(USER_ID, "month", FIXED_TODAY, null);

        assertThat(resp.getIncomeByMonth()).hasSize(12);
        assertThat(resp.getSpendByMonth()).hasSize(12);
        // oldest bucket first
        assertThat(resp.getIncomeByMonth().get(0).getMonth()).isEqualTo(YearMonth.of(2025, 7));
        assertThat(resp.getIncomeByMonth().get(0).getTotal()).isEqualByComparingTo("0");
        // 2025-08 credit got mapped
        assertThat(resp.getIncomeByMonth().get(1).getMonth()).isEqualTo(YearMonth.of(2025, 8));
        assertThat(resp.getIncomeByMonth().get(1).getTotal()).isEqualByComparingTo("5000.00");
        // current month credit + debit both pivot
        assertThat(resp.getIncomeByMonth().get(11).getMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(resp.getIncomeByMonth().get(11).getTotal()).isEqualByComparingTo("56800.00");
        assertThat(resp.getSpendByMonth().get(11).getMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(resp.getSpendByMonth().get(11).getTotal()).isEqualByComparingTo("12000.00");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static Category category(UUID id, String name) {
        return Category.builder().categoryId(id).userId(USER_ID).name(name).build();
    }

    private static CategorySpendAggregate spend(UUID categoryId, String total, long txnCount) {
        return new CategorySpendAggregate() {
            @Override public UUID getCategoryId() { return categoryId; }
            @Override public BigDecimal getTotal() { return new BigDecimal(total); }
            @Override public long getTxnCount() { return txnCount; }
        };
    }

    private static MonthlyTypeAggregate monthly(int year, int month, EntryType type, String total) {
        return new MonthlyTypeAggregate() {
            @Override public int getYear() { return year; }
            @Override public int getMonth() { return month; }
            @Override public EntryType getEntryType() { return type; }
            @Override public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    private static MerchantSpendAggregate merchant(String description, String total, long txnCount) {
        return new MerchantSpendAggregate() {
            @Override public String getDescription() { return description; }
            @Override public BigDecimal getTotal() { return new BigDecimal(total); }
            @Override public long getTxnCount() { return txnCount; }
        };
    }

    private static AccountBalanceAggregate balance(String totalCredit, String totalDebit) {
        return new AccountBalanceAggregate() {
            @Override public UUID getAccountId() { return UUID.randomUUID(); }
            @Override public String getCurrency() { return "HKD"; }
            @Override public BigDecimal getTotalCredit() { return new BigDecimal(totalCredit); }
            @Override public BigDecimal getTotalDebit() { return new BigDecimal(totalDebit); }
            @Override public long getTxnCount() { return 1L; }
            @Override public LocalDate getLastTxnDate() { return LocalDate.now(); }
        };
    }

    @SuppressWarnings("unused")
    private static Collection<UUID> none() { return null; }
}
