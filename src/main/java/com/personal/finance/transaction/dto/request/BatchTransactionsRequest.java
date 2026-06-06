package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Body for {@code POST /v1/transactions/batch} (internal) — spec §3.2. */
@Data
@NoArgsConstructor
@Schema(description = "Request body for the internal batch-insert endpoint. Contains a bulk-upload job reference and up to 500 transaction rows.")
public class BatchTransactionsRequest {

    @NotNull
    @Schema(description = "UUID of the originating bulk-upload job used for tracing and idempotency",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID bulkJobId;

    @NotEmpty
    @Size(max = 500, message = "batch size must not exceed 500 rows")
    @Valid
    @Schema(description = "Transaction rows to insert — minimum 1, maximum 500",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Row> rows;

    @Data
    @NoArgsConstructor
    @Schema(description = "A single transaction row within a batch-insert request.")
    public static class Row {

        @NotNull
        @Schema(description = "UUID of the account to post this row against",
                example = "d290f1ee-6c54-4b01-90e6-d701748f0851",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private UUID accountId;

        @NotNull
        @Schema(description = "Double-entry side: DEBIT or CREDIT",
                example = "CREDIT",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private EntryType entryType;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Schema(description = "Positive monetary amount — must be greater than zero",
                example = "1250.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal amount;

        @NotBlank
        @Size(min = 3, max = 3)
        @Schema(description = "ISO 4217 three-letter currency code",
                example = "GBP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String currency;

        @NotNull
        @Schema(description = "Date the transaction occurred, ISO-8601 (yyyy-MM-dd)",
                example = "2024-05-20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate transactionDate;

        @Size(max = 100)
        @Schema(description = "External reference or import identifier (max 100 chars)",
                example = "IMPORT-2024-ROW-0042")
        private String reference;

        @Schema(description = "Human-readable description from the import source",
                example = "Salary payment May 2024")
        private String description;

        @Schema(description = "Pre-resolved category UUID. Mutually exclusive with categoryName.",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private UUID categoryId;

        @Size(max = 100)
        @Schema(description = "Category name to resolve inline. Creates the category if absent. Mutually exclusive with categoryId.",
                example = "Income")
        private String categoryName;
    }
}
