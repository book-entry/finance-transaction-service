package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body for {@code PATCH /v1/transactions/{id}} — partial update of editable
 * fields ({@link #description}, {@link #reference}, {@link #transactionDate}).
 * All editable fields are optional — only non-null ones are applied.
 *
 * <p>The immutable fields below are declared so Jackson can bind them; if any
 * is non-null the service throws {@link
 * com.personal.finance.transaction.exception.ImmutableFieldUpdateException}
 * (422). Category changes go through {@code PATCH /v1/transactions/{id}/category}.
 */
@Data
@NoArgsConstructor
@Schema(description = "Partial update body for a transaction. Only non-null fields are applied. "
        + "Setting any immutable field (accountId, entryType, amount, currency, source, categoryId, categoryName) returns 422.")
public class UpdateTransactionRequest {

    // ── editable ─────────────────────────────────────────────────────────
    @Schema(description = "Updated human-readable description",
            example = "Monthly gym membership")
    private String description;

    @Size(max = 100)
    @Schema(description = "Updated external reference or cheque number (max 100 chars)",
            example = "REF-20240701-042")
    private String reference;

    @Schema(description = "Updated transaction date, ISO-8601 (yyyy-MM-dd)",
            example = "2024-07-01")
    private LocalDate transactionDate;

    // ── immutable — any non-null value here yields a 422 ─────────────────
    @Schema(description = "IMMUTABLE — supplying this field returns 422. Use DELETE + POST to change the account.",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID accountId;

    @Schema(description = "IMMUTABLE — supplying this field returns 422.",
            example = "DEBIT")
    private EntryType entryType;

    @Schema(description = "IMMUTABLE — supplying this field returns 422.",
            example = "49.99")
    private BigDecimal amount;

    @Schema(description = "IMMUTABLE — supplying this field returns 422.",
            example = "USD")
    private String currency;

    @Schema(description = "IMMUTABLE — supplying this field returns 422.",
            example = "MANUAL")
    private Source source;

    @Schema(description = "IMMUTABLE — use PATCH /{id}/category to change the category.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID categoryId;

    @Schema(description = "IMMUTABLE — use PATCH /{id}/category to change the category.",
            example = "Groceries")
    private String categoryName;
}
