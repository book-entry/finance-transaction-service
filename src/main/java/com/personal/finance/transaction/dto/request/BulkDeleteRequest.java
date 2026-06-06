package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Body for {@code DELETE /v1/transactions/bulk}. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for bulk soft-deletion of transactions.")
public class BulkDeleteRequest {

    @NotEmpty
    @Size(max = 500, message = "bulk delete size must not exceed 500 rows")
    @Schema(description = "UUIDs of the transactions to soft-delete — minimum 1, maximum 500",
            example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"4fb96a75-6828-5673-c4gd-3d074g77bgb7\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> transactionIds;
}
