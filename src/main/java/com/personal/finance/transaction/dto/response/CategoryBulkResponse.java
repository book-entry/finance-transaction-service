package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Response for {@code POST /v1/categories/bulk} — spec §3.3. */
@Value
@Builder
@Schema(description = "Result of a bulk category upsert operation.")
public class CategoryBulkResponse {

    @Schema(description = "Number of new categories inserted",
            example = "4")
    int created;

    @Schema(description = "Number of items skipped because a category with the same name already existed, or were duplicate names within the request",
            example = "1")
    int skipped;

    @Schema(description = "Resolved (categoryId, name) pairs for all categories — both newly created and pre-existing — referenced by the request")
    List<Item> categories;

    @Value
    @Builder
    @Schema(description = "Minimal category identifier returned in a bulk upsert response.")
    public static class Item {

        @Schema(description = "UUID of the category",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID categoryId;

        @Schema(description = "Display name of the category",
                example = "Travel")
        String name;
    }
}
