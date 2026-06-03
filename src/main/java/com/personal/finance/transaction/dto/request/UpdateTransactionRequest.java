package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
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
public class UpdateTransactionRequest {

    // ── editable ─────────────────────────────────────────────────────────
    private String description;
    @Size(max = 100)
    private String reference;
    private LocalDate transactionDate;

    // ── immutable — any non-null value here yields a 422 ─────────────────
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private Source source;
    private UUID categoryId;
    private String categoryName;
}
