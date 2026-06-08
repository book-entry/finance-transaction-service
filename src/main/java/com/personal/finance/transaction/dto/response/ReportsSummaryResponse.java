package com.personal.finance.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Response for {@code GET /v1/reports/summary} — one-shot dashboard /
 * reports aggregate. See REQ-reports-summary.md §2.2 for the wire shape.
 *
 * <p>Null fields are emitted as JSON {@code null} (not omitted) — the
 * frontend reads {@code currency == null} as the multi-currency banner
 * trigger and {@code netWorth.delta == null} as "can't compute".
 */
@Value
@Builder
@Schema(description = "One-shot Dashboard / Reports summary aggregate for an authenticated user.")
public class ReportsSummaryResponse {

    @Schema(description = "Echo of the requested range — month or year.", example = "month")
    String range;

    @Schema(description = "Echo of the asOf cut-off, ISO-8601.", example = "2026-06-04")
    LocalDate asOf;

    /**
     * {@code null} when transactions in scope span more than one currency
     * (and also when there are zero transactions). When non-null, every
     * monetary value in the response is denominated in this currency.
     */
    @Schema(description = "ISO 4217 currency code shared by all transactions in scope, or null when mixed / empty.",
            example = "HKD", nullable = true)
    String currency;

    NetWorth netWorth;

    @Schema(description = "DEBIT spend grouped by category for the requested range; uncategorised rows surface with categoryId/name = null.")
    List<CategorySpend> spendByCategory;

    @Schema(description = "12 calendar-month CREDIT trend ending at the month of asOf, oldest first. Empty months emit total=0.00.")
    List<MonthlyTotal> incomeByMonth;

    @Schema(description = "12 calendar-month DEBIT trend ending at the month of asOf, oldest first. Empty months emit total=0.00.")
    List<MonthlyTotal> spendByMonth;

    @Schema(description = "Up to 5 merchants by DEBIT spend in the requested range, sorted total DESC.")
    List<MerchantSpend> topMerchants;

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "Net worth as of cutoff, compared to the equivalent point one range ago.")
    public static class NetWorth {

        @Schema(description = "Credit minus debit across all transactions on or before asOf. Null when currencies are mixed.",
                example = "12345.67", nullable = true)
        BigDecimal current;

        @Schema(description = "Same aggregation as current but rewound by one month (range=month) or one year (range=year). Null when currencies are mixed.",
                example = "11200.00", nullable = true)
        BigDecimal previous;

        @Schema(description = "current minus previous. Null when either side is null.",
                example = "1145.67", nullable = true)
        BigDecimal delta;
    }

    @Value
    @Builder
    @Schema(description = "Category-level DEBIT spend bucket.")
    public static class CategorySpend {

        @Schema(description = "Category UUID, or null for the uncategorised bucket.",
                example = "9c3a0b22-8f10-4f1d-87a8-9e6a8a5d8a01", nullable = true)
        UUID categoryId;

        @Schema(description = "Category name, or null when the category was soft-deleted or this is the uncategorised bucket.",
                example = "Groceries", nullable = true)
        String name;

        @Schema(description = "Sum of DEBIT amounts in scope for this category.",
                example = "1234.50")
        BigDecimal total;

        @Schema(description = "Number of DEBIT transactions in scope for this category.",
                example = "8")
        long txnCount;
    }

    @Value
    @Builder
    @Schema(description = "One bucket in a 12-month trend array.")
    public static class MonthlyTotal {

        @Schema(description = "Calendar month, ISO-8601 (YYYY-MM).", example = "2025-08")
        @JsonFormat(pattern = "yyyy-MM")
        YearMonth month;

        @Schema(description = "Sum of amounts in this month bucket.", example = "56800.00")
        BigDecimal total;
    }

    @Value
    @Builder
    @Schema(description = "Top-merchant DEBIT spend bucket.")
    public static class MerchantSpend {

        @Schema(description = "Most-recent original description for this merchant — preserves case/whitespace.",
                example = "ParknShop")
        String description;

        @Schema(description = "Sum of DEBIT amounts in scope for this merchant.", example = "1450.20")
        BigDecimal total;

        @Schema(description = "Number of DEBIT transactions in scope for this merchant.", example = "12")
        long txnCount;
    }
}
