package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Response for {@code DELETE /v1/transactions/bulk}. */
@Value
@Builder
public class BulkDeleteResponse {
    int deleted;
    List<UUID> notFound;
}
