package com.personal.finance.transaction.dto.request;

import com.personal.finance.transaction.enums.EntryType;
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
public class BatchTransactionsRequest {

    @NotNull
    private UUID bulkJobId;

    @NotEmpty
    @Size(max = 500, message = "batch size must not exceed 500 rows")
    @Valid
    private List<Row> rows;

    @Data
    @NoArgsConstructor
    public static class Row {
        @NotNull
        private UUID accountId;
        @NotNull
        private EntryType entryType;
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal amount;
        @NotBlank
        @Size(min = 3, max = 3)
        private String currency;
        @NotNull
        private LocalDate transactionDate;
        @Size(max = 100)
        private String reference;
        private String description;
        /** Optional — pre-resolved category id (no inline create in batch). */
        private UUID categoryId;
    }
}
