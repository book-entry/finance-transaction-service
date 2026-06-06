package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Body for {@code PATCH /v1/transactions/bulk-category}.
 * Exactly one of {@link #categoryId} / {@link #categoryName} must be provided —
 * same semantics as the single PATCH endpoint. The 500-row cap matches the
 * batch-insert cap so the frontend can chunk identically.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request body for bulk re-categorisation. "
        + "Supply a list of transaction UUIDs and exactly one of categoryId or categoryName.")
public class BulkCategoryRequest {

    @NotEmpty
    @Size(max = 500, message = "bulk category update size must not exceed 500 rows")
    @Schema(description = "UUIDs of the transactions to re-categorise — minimum 1, maximum 500",
            example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"4fb96a75-6828-5673-c4gd-3d074g77bgb7\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> transactionIds;

    @Schema(description = "UUID of an existing category to assign. Mutually exclusive with categoryName.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID categoryId;

    @Size(max = 100)
    @Schema(description = "Name of a category to assign. Creates the category inline if it does not already exist. Mutually exclusive with categoryId.",
            example = "Transport")
    private String categoryName;
}
