package com.personal.finance.transaction.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-category DEBIT aggregate row for {@code GET /v1/reports/summary}'s
 * {@code spendByCategory}. {@code categoryId} is {@code null} for the
 * uncategorised bucket (rows whose {@code category_id} was NULL at write
 * time, or whose category was soft-deleted).
 */
public interface CategorySpendAggregate {
    UUID getCategoryId();
    BigDecimal getTotal();
    long getTxnCount();
}
