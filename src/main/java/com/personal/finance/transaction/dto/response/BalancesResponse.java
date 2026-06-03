package com.personal.finance.transaction.dto.response;

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
public class BalancesResponse {
    LocalDate asOf;
    List<AccountBalance> balances;

    @Value
    @Builder
    public static class AccountBalance {
        UUID accountId;
        String currency;
        BigDecimal totalCredit;
        BigDecimal totalDebit;
        /** {@code totalCredit - totalDebit} — does not include opening balance. */
        BigDecimal balance;
        long txnCount;
        LocalDate lastTxnDate;
    }
}
