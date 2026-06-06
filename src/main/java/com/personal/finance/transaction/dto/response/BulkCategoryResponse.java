package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Result of a bulk re-categorisation operation.")
public class BulkCategoryResponse {

    @Schema(description = "Number of transactions whose category was changed",
            example = "45")
    int updated;

    @Schema(description = "Number of transactions that were already in the target category and required no change",
            example = "3")
    int skipped;

    @Schema(description = "UUIDs from the request that could not be found, were already soft-deleted, or are not owned by the user")
    List<UUID> notFound;

    @Schema(description = "The category that was applied to the updated transactions")
    CategoryRefResponse category;
}
