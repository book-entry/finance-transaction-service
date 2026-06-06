package com.personal.finance.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Embedded category descriptor for {@code TransactionResponse.category}.
 * {@code isNew} is set by the inline-create logic in spec §3.2 PATCH and
 * §2.4 — true if this PATCH inserted the category, false otherwise.
 */
@Value
@Builder
@Schema(description = "Lightweight category reference embedded in transaction and categorisation responses.")
public class CategoryRefResponse {

    @Schema(description = "UUID of the category",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    UUID id;

    @Schema(description = "Display name of the category",
            example = "Groceries")
    String name;

    /** Pinned via {@link JsonProperty} so Lombok's {@code isNew()} getter doesn't drop the prefix. */
    @JsonProperty("isNew")
    @Schema(description = "True when this category was created inline by the current request; false when it already existed",
            example = "false")
    boolean isNew;
}
