package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response for category endpoints — spec §3.3. */
@Value
@Builder
public class CategoryResponse {
    UUID categoryId;
    String userId;
    String name;
    String colourHex;
    OffsetDateTime createdAt;
}
