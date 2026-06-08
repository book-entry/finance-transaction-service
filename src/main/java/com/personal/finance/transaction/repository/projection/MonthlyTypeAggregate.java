package com.personal.finance.transaction.repository.projection;

import com.personal.finance.transaction.enums.EntryType;

import java.math.BigDecimal;

/**
 * Year/month bucket grouped by {@link EntryType} for {@code GET /v1/reports/summary}'s
 * {@code incomeByMonth} (CREDIT) and {@code spendByMonth} (DEBIT) trend arrays.
 * One GROUP BY covers both directions — the service pivots into two 12-entry
 * arrays.
 */
public interface MonthlyTypeAggregate {
    int getYear();
    int getMonth();
    EntryType getEntryType();
    BigDecimal getTotal();
}
