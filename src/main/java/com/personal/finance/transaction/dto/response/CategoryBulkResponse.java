package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Response for {@code POST /v1/categories/bulk} — spec §3.3. */
@Value
@Builder
public class CategoryBulkResponse {
    int created;
    int skipped;
    List<Item> categories;

    @Value
    @Builder
    public static class Item {
        UUID categoryId;
        String name;
    }
}
