package com.personal.finance.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response for transaction endpoints — spec §3.2. */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full representation of a single transaction.")
public class TransactionResponse {

    @Schema(description = "Unique identifier of the transaction",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID transactionId;

    @Schema(description = "UUID of the account this transaction belongs to",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID accountId;

    @Schema(description = "Double-entry side — DEBIT reduces balance, CREDIT increases it",
            example = "DEBIT")
    EntryType entryType;

    @Schema(description = "Positive monetary amount",
            example = "49.99")
    BigDecimal amount;

    @Schema(description = "ISO 4217 three-letter currency code",
            example = "USD")
    String currency;

    @Schema(description = "Date the transaction occurred, ISO-8601 (yyyy-MM-dd)",
            example = "2024-06-15")
    LocalDate transactionDate;

    @Schema(description = "External reference or cheque number",
            example = "REF-20240615-001")
    String reference;

    @Schema(description = "Human-readable description of the transaction",
            example = "Weekly grocery shop at Whole Foods")
    String description;

    @Schema(description = "Origin of the transaction row — MANUAL, BULK, or API",
            example = "MANUAL")
    Source source;

    @Schema(description = "Category assigned to this transaction. Null when the transaction is uncategorised or its category was deleted.")
    CategoryRefResponse category;

    @Schema(description = "ISO-8601 timestamp when the transaction was recorded",
            example = "2024-06-15T10:30:00Z")
    OffsetDateTime createdAt;
}
