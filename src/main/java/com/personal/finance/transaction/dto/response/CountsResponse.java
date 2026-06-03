package com.personal.finance.transaction.dto.response;

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
public class CountsResponse {
    long total;
    long uncategorized;
    /** Per-category active transaction count — empty map if the user has none. */
    Map<UUID, Long> byCategory;
}
