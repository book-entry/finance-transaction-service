package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * Response for {@code PATCH /v1/transactions/{id}/category} — spec §3.2 returns
 * both the updated transaction and the category descriptor (which exposes
 * {@code isNew} so the UI knows whether to refresh the category list).
 */
@Value
@Builder
public class CategorisedTransactionResponse {
    TransactionResponse transaction;
    CategoryRefResponse category;
}
