package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Response for {@code PATCH /v1/transactions/{id}/category} — spec §3.2 returns
 * both the updated transaction and the category descriptor (which exposes
 * {@code isNew} so the UI knows whether to refresh the category list).
 */
@Value
@Builder
@Schema(description = "Response for category-assignment on a single transaction. "
        + "Includes the updated transaction and the assigned category. "
        + "isNew=true on the category when it was created inline by this request.")
public class CategorisedTransactionResponse {

    @Schema(description = "The transaction with its category updated")
    TransactionResponse transaction;

    @Schema(description = "The category that was assigned. isNew=true when created inline by this request.")
    CategoryRefResponse category;
}
