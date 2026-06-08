package com.personal.finance.transaction.repository.projection;

import java.math.BigDecimal;

/**
 * One row per merchant in {@code GET /v1/reports/summary}'s {@code topMerchants}.
 * {@code description} is the most-recent original variant of the merchant's
 * description (preserved capitalisation/whitespace); aggregation is over the
 * normalised key {@code LOWER(TRIM(description))} so case/whitespace variants
 * fold together.
 */
public interface MerchantSpendAggregate {
    String getDescription();
    BigDecimal getTotal();
    long getTxnCount();
}
