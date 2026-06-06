package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body for {@code PUT /v1/categories/{id}} — spec §3.3. Partial. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for updating a category. Only non-null supplied fields are applied.")
public class UpdateCategoryRequest {

    @Size(max = 100)
    @Schema(description = "New display name for the category — must be unique among the user's active categories (max 100 chars)",
            example = "Food & Drink")
    private String name;

    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "colourHex must be a 7-char hex e.g. #4ade80")
    @Size(max = 7)
    @Schema(description = "New CSS hex colour code — must match #RRGGBB format",
            example = "#f97316")
    private String colourHex;
}
