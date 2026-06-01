package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/** Response for {@code GET /v1/categories/{id}/summary} — spec §3.3. */
@Value
@Builder
public class CategorySummaryResponse {
    UUID categoryId;
    String categoryName;
    long transactionCount;
    BigDecimal totalAmount;
    /** Currency of the first transaction encountered, or null if no transactions. */
    String currency;
}
