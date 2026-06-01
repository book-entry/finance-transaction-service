package com.personal.finance.transaction.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Body for {@code PATCH /v1/transactions/{id}/category} — spec §3.2.
 * Exactly one of {@link #categoryId} / {@link #categoryName} must be provided.
 * Service throws {@code InvalidCategoryRequestException} (400) otherwise.
 */
@Data
@NoArgsConstructor
public class CategorisePatchRequest {

    private UUID categoryId;

    @Size(max = 100)
    private String categoryName;
}
