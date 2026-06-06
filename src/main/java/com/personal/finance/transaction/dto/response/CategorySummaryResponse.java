package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/** Response for {@code GET /v1/categories/{id}/summary} — spec §3.3. */
@Value
@Builder
@Schema(description = "Aggregate statistics for a single category, used primarily by the delete-confirmation dialog.")
public class CategorySummaryResponse {

    @Schema(description = "UUID of the category",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID categoryId;

    @Schema(description = "Display name of the category",
            example = "Groceries")
    String categoryName;

    @Schema(description = "Number of active transactions currently assigned to this category",
            example = "37")
    long transactionCount;

    @Schema(description = "Sum of all active transaction amounts in this category. Zero when transactionCount is 0.",
            example = "1842.75")
    BigDecimal totalAmount;

    @Schema(description = "ISO 4217 currency code sampled from the first transaction encountered. Null when transactionCount is 0.",
            example = "USD")
    String currency;
}
