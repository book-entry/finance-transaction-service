package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Response for {@code PATCH /v1/transactions/bulk-category}.
 * <ul>
 *   <li>{@code updated} — rows whose categoryId actually changed.</li>
 *   <li>{@code skipped} — active+owned rows already in the target category
 *       (no-op).</li>
 *   <li>{@code notFound} — requested ids that don't exist, were already
 *       soft-deleted, or aren't owned by this user.</li>
 * </ul>
 * {@code updated + skipped + notFound.size() == transactionIds.size()} on a
 * stable read; a concurrent delete may shift one from {@code updated} to
 * {@code notFound} but the math stays honest.
 */
@Value
@Builder
public class BulkCategoryResponse {
    int updated;
    int skipped;
    List<UUID> notFound;
    CategoryRefResponse category;
}
