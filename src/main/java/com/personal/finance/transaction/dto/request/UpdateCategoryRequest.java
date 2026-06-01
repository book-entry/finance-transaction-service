package com.personal.finance.transaction.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body for {@code PUT /v1/categories/{id}} — spec §3.3. Partial. */
@Data
@NoArgsConstructor
public class UpdateCategoryRequest {

    @Size(max = 100)
    private String name;

    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "colourHex must be a 7-char hex e.g. #4ade80")
    @Size(max = 7)
    private String colourHex;
}
