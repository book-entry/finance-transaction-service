package com.personal.finance.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Single item in the {@code POST /v1/categories/bulk} payload — spec §3.3. */
@Data
@NoArgsConstructor
@Schema(description = "A single category entry within a bulk upsert request.")
public class BulkCategoryItem {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Display name for the category (max 100 chars). Duplicate names within the request are de-duplicated.",
            example = "Travel",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "colourHex must be a 7-char hex")
    @Size(max = 7)
    @Schema(description = "Optional CSS hex colour code — must match #RRGGBB format",
            example = "#3b82f6")
    private String colourHex;
}
