package com.personal.finance.transaction.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response for {@code GET /v1/transactions} — spec §3.2 paged. */
@Value
@Builder
public class TransactionPageResponse {
    List<TransactionResponse> data;
    long total;
    int page;
    int size;
}
