package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response for {@code POST /v1/transactions/batch} (internal) — spec §3.2. */
@Value
@Builder
public class BatchInsertResponse {
    int insertedCount;
    List<FailedRow> failedRows;

    @Value
    @Builder
    public static class FailedRow {
        int rowIndex;
        String reason;
    }
}
