package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response for {@code POST /v1/transactions/batch} (internal) — spec §3.2. */
@Value
@Builder
@Schema(description = "Result of a batch-insert operation. Rows that could not be inserted are listed individually.")
public class BatchInsertResponse {

    @Schema(description = "Number of rows successfully inserted",
            example = "498")
    int insertedCount;

    @Schema(description = "Rows that could not be inserted, with the reason for each failure")
    List<FailedRow> failedRows;

    @Value
    @Builder
    @Schema(description = "Details of a single row that failed to insert during a batch operation.")
    public static class FailedRow {

        @Schema(description = "0-based index of the row in the original request list",
                example = "7")
        int rowIndex;

        @Schema(description = "Human-readable reason the row was rejected",
                example = "Account d290f1ee-6c54-4b01-90e6-d701748f0851 is CLOSED")
        String reason;
    }
}
