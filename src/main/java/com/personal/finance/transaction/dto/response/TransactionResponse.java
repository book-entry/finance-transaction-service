package com.personal.finance.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
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
public class TransactionResponse {
    UUID transactionId;
    UUID accountId;
    EntryType entryType;
    BigDecimal amount;
    String currency;
    LocalDate transactionDate;
    String reference;
    String description;
    Source source;
    /** Null when the transaction is uncategorised or its category was deleted. */
    CategoryRefResponse category;
    OffsetDateTime createdAt;
}
