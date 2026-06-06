package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Body for {@code POST /v1/transactions} — spec §3.2. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for creating a new transaction. Supply either categoryId or categoryName — not both.")
public class CreateTransactionRequest {

    @NotNull
    @Schema(description = "UUID of the account to post this transaction against",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID accountId;

    @NotNull
    @Schema(description = "Double-entry side: DEBIT reduces the account balance, CREDIT increases it",
            example = "DEBIT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private EntryType entryType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    @Schema(description = "Positive monetary amount — must be greater than zero",
            example = "49.99",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    @Schema(description = "ISO 4217 three-letter currency code",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotNull
    @Schema(description = "Date the transaction occurred, ISO-8601 (yyyy-MM-dd)",
            example = "2024-06-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate transactionDate;

    @Size(max = 100)
    @Schema(description = "External reference or cheque number (max 100 chars)",
            example = "REF-20240615-001")
    private String reference;

    @Schema(description = "Human-readable description of the transaction",
            example = "Weekly grocery shop at Whole Foods")
    private String description;

    @Schema(description = "UUID of an existing category to assign. Mutually exclusive with categoryName.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID categoryId;

    @Size(max = 100)
    @Schema(description = "Name of a category to assign. Creates the category inline if it does not already exist. Mutually exclusive with categoryId.",
            example = "Groceries")
    private String categoryName;
}
