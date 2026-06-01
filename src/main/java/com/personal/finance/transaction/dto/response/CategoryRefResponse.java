package com.personal.finance.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CategoryRefResponse {
    UUID id;
    String name;
    /** Pinned via {@link JsonProperty} so Lombok's {@code isNew()} getter doesn't drop the prefix. */
    @JsonProperty("isNew")
    boolean isNew;
}
