package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body for {@code POST /v1/categories} — spec §3.3. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for creating a new spending category.")
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Display name for the category — must be unique among the user's active categories (max 100 chars)",
            example = "Groceries",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "colourHex must be a 7-char hex e.g. #4ade80")
    @Size(max = 7)
    @Schema(description = "Optional CSS hex colour code used for UI rendering — must match #RRGGBB format",
            example = "#4ade80")
    private String colourHex;
}
