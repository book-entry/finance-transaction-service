package com.personal.finance.transaction.dto.request;

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
public class BulkCategoryRequest {

    @NotEmpty
    @Size(max = 500, message = "bulk category update size must not exceed 500 rows")
    private List<UUID> transactionIds;

    private UUID categoryId;

    @Size(max = 100)
    private String categoryName;
}
