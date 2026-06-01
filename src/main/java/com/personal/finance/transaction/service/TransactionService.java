package com.personal.finance.transaction.service;

import com.personal.finance.transaction.dto.request.BatchTransactionsRequest;
import com.personal.finance.transaction.dto.request.CategorisePatchRequest;
import com.personal.finance.transaction.dto.request.CreateTransactionRequest;
import com.personal.finance.transaction.dto.response.BatchInsertResponse;
import com.personal.finance.transaction.dto.response.CategorisedTransactionResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.UUID;

/** Implements the transaction flows defined in spec §3.2. */
public interface TransactionService {

    /** Spec §3.2 POST /v1/transactions. */
    TransactionResponse createTransaction(String userId, CreateTransactionRequest request);

    /** Spec §3.2 GET /v1/transactions — paged & filtered. */
    TransactionPageResponse listTransactions(String userId, UUID accountId, UUID categoryId,
                                             LocalDate from, LocalDate to, int page, int size);

    /** Spec §3.2 GET /v1/transactions/{id}. */
    TransactionResponse getTransaction(String userId, UUID transactionId);

    /** Spec §3.2 PATCH /v1/transactions/{id}/category. */
    CategorisedTransactionResponse categorise(String userId, UUID transactionId, CategorisePatchRequest request);

    /** Spec §3.2 DELETE /v1/transactions/{id}. */
    void deleteTransaction(String userId, UUID transactionId);

    /** Spec §3.2 POST /v1/transactions/batch — internal bulk insert. */
    BatchInsertResponse insertBatch(String userId, BatchTransactionsRequest request);
}
