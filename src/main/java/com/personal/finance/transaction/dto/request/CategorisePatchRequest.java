package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for assigning a category to a transaction. "
        + "Supply exactly one of categoryId (existing) or categoryName (inline create). "
        + "Providing both or neither returns 400.")
public class CategorisePatchRequest {

    @Schema(description = "UUID of an existing category to assign. Mutually exclusive with categoryName.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID categoryId;

    @Size(max = 100)
    @Schema(description = "Name of a category to assign. Creates the category inline if it does not already exist. Mutually exclusive with categoryId.",
            example = "Dining Out")
    private String categoryName;
}
