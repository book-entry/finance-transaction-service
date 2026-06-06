package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response for {@code GET /v1/transactions/balances}.
 * Aggregates per-account credit/debit totals computed from active transactions
 * dated on or before {@code asOf}. Excludes any opening balance held by the
 * account service — callers add that separately so the two services stay
 * orthogonal.
 */
@Value
@Builder
@Schema(description = "Per-account balance aggregates computed from active transactions up to the asOf date.")
public class BalancesResponse {

    @Schema(description = "The cut-off date used for the aggregation — transactions after this date are excluded. ISO-8601 (yyyy-MM-dd).",
            example = "2024-12-31")
    LocalDate asOf;

    @Schema(description = "Balance breakdown per account")
    List<AccountBalance> balances;

    @Value
    @Builder
    @Schema(description = "Credit/debit aggregates for a single account.")
    public static class AccountBalance {

        @Schema(description = "UUID of the account",
                example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID accountId;

        @Schema(description = "ISO 4217 currency code of the transactions included in this aggregate",
                example = "USD")
        String currency;

        @Schema(description = "Sum of all CREDIT transaction amounts for this account",
                example = "5000.00")
        BigDecimal totalCredit;

        @Schema(description = "Sum of all DEBIT transaction amounts for this account",
                example = "3200.50")
        BigDecimal totalDebit;

        @Schema(description = "Net balance — totalCredit minus totalDebit. Does not include the account's opening balance.",
                example = "1799.50")
        BigDecimal balance;

        @Schema(description = "Total number of active transactions included in this aggregate",
                example = "27")
        long txnCount;

        @Schema(description = "Date of the most recent active transaction for this account, ISO-8601 (yyyy-MM-dd)",
                example = "2024-12-28")
        LocalDate lastTxnDate;
    }
}
