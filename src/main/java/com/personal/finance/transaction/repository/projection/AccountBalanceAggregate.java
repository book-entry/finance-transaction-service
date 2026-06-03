package com.personal.finance.transaction.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-account aggregate row returned by
 * {@link com.personal.finance.transaction.repository.TransactionRepository#aggregateBalances}.
 * Grouped by {@code (accountId, currency)} so dirty data that mixes currencies
 * within one account surfaces as separate rows rather than being silently
 * collapsed.
 */
public interface AccountBalanceAggregate {
    UUID getAccountId();
    String getCurrency();
    BigDecimal getTotalCredit();
    BigDecimal getTotalDebit();
    long getTxnCount();
    LocalDate getLastTxnDate();
}
