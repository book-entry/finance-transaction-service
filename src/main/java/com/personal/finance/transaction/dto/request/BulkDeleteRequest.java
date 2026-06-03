package com.personal.finance.transaction.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Body for {@code DELETE /v1/transactions/bulk}. */
@Data
@NoArgsConstructor
public class BulkDeleteRequest {

    @NotEmpty
    @Size(max = 500, message = "bulk delete size must not exceed 500 rows")
    private List<UUID> transactionIds;
}
