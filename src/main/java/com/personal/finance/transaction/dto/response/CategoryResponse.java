package com.personal.finance.transaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response for category endpoints — spec §3.3. */
@Value
@Builder
@Schema(description = "Full representation of a user-defined spending category.")
public class CategoryResponse {

    @Schema(description = "Unique identifier of the category",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID categoryId;

    @Schema(description = "ID of the user who owns this category",
            example = "user_abc123")
    String userId;

    @Schema(description = "Display name of the category",
            example = "Groceries")
    String name;

    @Schema(description = "CSS hex colour code used for UI rendering, in #RRGGBB format",
            example = "#4ade80")
    String colourHex;

    @Schema(description = "ISO-8601 timestamp when the category was created",
            example = "2024-01-10T08:00:00Z")
    OffsetDateTime createdAt;
}
