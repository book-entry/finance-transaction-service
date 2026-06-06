package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Response for {@code DELETE /v1/transactions/bulk}. */
@Value
@Builder
@Schema(description = "Result of a bulk soft-deletion operation.")
public class BulkDeleteResponse {

    @Schema(description = "Number of transactions that were successfully soft-deleted",
            example = "48")
    int deleted;

    @Schema(description = "UUIDs from the request that could not be found, were already soft-deleted, or are not owned by the user")
    List<UUID> notFound;
}
