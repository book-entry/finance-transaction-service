package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

/**
 * Response for {@code GET /v1/transactions/counts} — feeds the sidebar
 * "Transactions [N]" badge and the honest "Uncategorized" chip count.
 * {@code total = uncategorized + sum(byCategory.values())}.
 */
@Value
@Builder
@Schema(description = "Active transaction counts for the authenticated user — total, uncategorised, and per-category.")
public class CountsResponse {

    @Schema(description = "Total number of active transactions owned by the user",
            example = "215")
    long total;

    @Schema(description = "Number of active transactions not assigned to any category",
            example = "42")
    long uncategorized;

    @Schema(description = "Map of category UUID to active transaction count for that category. Empty map if the user has no categorised transactions.",
            example = "{\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\": 37, \"b2c3d4e5-f6a7-8901-bcde-f01234567891\": 12}")
    Map<UUID, Long> byCategory;
}
