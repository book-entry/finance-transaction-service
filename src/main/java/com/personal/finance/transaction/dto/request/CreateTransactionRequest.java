package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
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
public class CreateTransactionRequest {

    @NotNull
    private UUID accountId;

    @NotNull
    private EntryType entryType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private LocalDate transactionDate;

    @Size(max = 100)
    private String reference;

    private String description;

    /** Optional — use {@link #categoryName} for inline create. Mutually exclusive. */
    private UUID categoryId;

    /** Optional — inline create if not already present. Mutually exclusive with {@link #categoryId}. */
    @Size(max = 100)
    private String categoryName;
}
