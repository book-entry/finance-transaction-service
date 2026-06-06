package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response for {@code GET /v1/transactions} — spec §3.2 paged. */
@Value
@Builder
@Schema(description = "Paginated list of transactions.")
public class TransactionPageResponse {

    @Schema(description = "Transactions on the current page")
    List<TransactionResponse> data;

    @Schema(description = "Total number of transactions matching the applied filters",
            example = "342")
    long total;

    @Schema(description = "Current 1-based page number",
            example = "1")
    int page;

    @Schema(description = "Number of items per page",
            example = "50")
    int size;
}
