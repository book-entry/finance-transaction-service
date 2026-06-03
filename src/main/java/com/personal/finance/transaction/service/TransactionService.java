package com.personal.finance.transaction.service;

import com.personal.finance.transaction.dto.request.BatchTransactionsRequest;
import com.personal.finance.transaction.dto.request.BulkCategoryRequest;
import com.personal.finance.transaction.dto.request.BulkDeleteRequest;
import com.personal.finance.transaction.dto.request.CategorisePatchRequest;
import com.personal.finance.transaction.dto.request.CreateTransactionRequest;
import com.personal.finance.transaction.dto.response.BalancesResponse;
import com.personal.finance.transaction.dto.response.BatchInsertResponse;
import com.personal.finance.transaction.dto.response.BulkCategoryResponse;
import com.personal.finance.transaction.dto.response.BulkDeleteResponse;
import com.personal.finance.transaction.dto.response.CategorisedTransactionResponse;
import com.personal.finance.transaction.dto.response.CountsResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

/** Implements the transaction flows defined in spec §3.2. */
public interface TransactionService {

    /** Spec §3.2 POST /v1/transactions. */
    TransactionResponse createTransaction(String userId, CreateTransactionRequest request);

    /**
     * Spec §3.2 GET /v1/transactions — paged &amp; filtered.
     * <p>Category filter mutual-exclusion is enforced here: at most one of
     * {@code categoryId}, {@code categoryIds}, or {@code uncategorized=true}
     * may be set; passing more than one yields a 400.
     */
    TransactionPageResponse listTransactions(String userId,
                                             UUID accountId,
                                             UUID categoryId,
                                             Collection<UUID> categoryIds,
                                             boolean uncategorized,
                                             LocalDate from,
                                             LocalDate to,
                                             String q,
                                             int page,
                                             int size);

    /** Spec §3.2 GET /v1/transactions/{id}. */
    TransactionResponse getTransaction(String userId, UUID transactionId);

    /** Spec §3.2 PATCH /v1/transactions/{id}/category. */
    CategorisedTransactionResponse categorise(String userId, UUID transactionId, CategorisePatchRequest request);

    /** Spec §3.2 DELETE /v1/transactions/{id}. */
    void deleteTransaction(String userId, UUID transactionId);

    /** Spec §3.2 POST /v1/transactions/batch — internal bulk insert. */
    BatchInsertResponse insertBatch(String userId, BatchTransactionsRequest request);

    /**
     * {@code GET /v1/transactions/balances} — per-account credit/debit totals.
     * {@code asOf} defaults to today when null; {@code accountIds} is optional
     * (null/empty means "all owned accounts").
     */
    BalancesResponse listBalances(String userId, LocalDate asOf, Collection<UUID> accountIds);

<<<<<<< Updated upstream
    /** {@code PATCH /v1/transactions/bulk-category} — atomic bulk re-categorise. */
    BulkCategoryResponse bulkSetCategory(String userId, BulkCategoryRequest request);

    /** {@code DELETE /v1/transactions/bulk} — atomic bulk soft-delete. */
    BulkDeleteResponse bulkDelete(String userId, BulkDeleteRequest request);
=======
    /**
     * {@code GET /v1/transactions/counts} — total / uncategorised / per-category
     * active transaction counts for this user. Single GROUP BY in the
     * repository; the service folds the {@code null} group into
     * {@code uncategorized} and sums the rest into {@code total}.
     */
    CountsResponse listCounts(String userId);
>>>>>>> Stashed changes
}
