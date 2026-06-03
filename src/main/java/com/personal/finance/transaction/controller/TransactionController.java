package com.personal.finance.transaction.controller;

import com.personal.finance.transaction.dto.request.BatchTransactionsRequest;
import com.personal.finance.transaction.dto.request.BulkCategoryRequest;
import com.personal.finance.transaction.dto.request.BulkDeleteRequest;
import com.personal.finance.transaction.dto.request.CategorisePatchRequest;
import com.personal.finance.transaction.dto.request.CreateTransactionRequest;
import com.personal.finance.transaction.dto.request.UpdateTransactionRequest;
import com.personal.finance.transaction.dto.response.BalancesResponse;
import com.personal.finance.transaction.dto.response.BatchInsertResponse;
import com.personal.finance.transaction.dto.response.BulkCategoryResponse;
import com.personal.finance.transaction.dto.response.BulkDeleteResponse;
import com.personal.finance.transaction.dto.response.CategorisedTransactionResponse;
import com.personal.finance.transaction.dto.response.CountsResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;
import com.personal.finance.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** REST entry points for transactions — spec §3.2. Routing only. */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final TransactionService transactionService;

    /** Spec §3.2 — {@code POST /v1/transactions}. Returns 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestHeader(USER_ID_HEADER) String userId,
                                      @Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(userId, request);
    }

    /**
     * Spec §3.2 — {@code GET /v1/transactions}.
     * <p>Filter knobs: {@code accountId}, {@code from/to} (date range), and a
     * single category filter chosen from {@code categoryId} / {@code categoryIds}
     * / {@code uncategorized=true} (mutually exclusive — 400 otherwise).
     * {@code q} is a case-insensitive LIKE on description + reference.
     */
    @GetMapping
    public TransactionPageResponse list(@RequestHeader(USER_ID_HEADER) String userId,
                                        @RequestParam(required = false) UUID accountId,
                                        @RequestParam(required = false) UUID categoryId,
                                        @RequestParam(required = false) List<UUID> categoryIds,
                                        @RequestParam(required = false, defaultValue = "false") boolean uncategorized,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false, defaultValue = "1") int page,
                                        @RequestParam(required = false, defaultValue = "50") int size) {
        return transactionService.listTransactions(
                userId, accountId, categoryId, categoryIds, uncategorized, from, to, q, page, size);
    }

    /**
     * Spec §3.2 — {@code POST /v1/transactions/batch} (internal).
     * Declared BEFORE {@code /{id}} so Spring picks the static path first.
     */
    @PostMapping("/batch")
    public BatchInsertResponse batch(@RequestHeader(USER_ID_HEADER) String userId,
                                     @Valid @RequestBody BatchTransactionsRequest request) {
        return transactionService.insertBatch(userId, request);
    }

    /**
     * {@code GET /v1/transactions/counts} — total / uncategorised /
     * per-category active transaction counts. Declared BEFORE {@code /{id}}
     * so Spring routes the literal path first.
     */
    @GetMapping("/counts")
    public CountsResponse counts(@RequestHeader(USER_ID_HEADER) String userId) {
        return transactionService.listCounts(userId);
    }

    /**
     * {@code GET /v1/transactions/balances} — per-account aggregates.
     * Declared BEFORE {@code /{id}} so Spring routes the literal path first.
     */
    @GetMapping("/balances")
    public BalancesResponse balances(@RequestHeader(USER_ID_HEADER) String userId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                     @RequestParam(required = false) List<UUID> accountIds) {
        return transactionService.listBalances(userId, asOf, accountIds);
    }

    /** {@code PATCH /v1/transactions/bulk-category} — atomic bulk re-categorise. */
    @PatchMapping("/bulk-category")
    public BulkCategoryResponse bulkCategory(@RequestHeader(USER_ID_HEADER) String userId,
                                             @Valid @RequestBody BulkCategoryRequest request) {
        return transactionService.bulkSetCategory(userId, request);
    }

    /** {@code DELETE /v1/transactions/bulk} — atomic bulk soft-delete. */
    @DeleteMapping("/bulk")
    public BulkDeleteResponse bulkDelete(@RequestHeader(USER_ID_HEADER) String userId,
                                         @Valid @RequestBody BulkDeleteRequest request) {
        return transactionService.bulkDelete(userId, request);
    }

    /** Spec §3.2 — {@code GET /v1/transactions/{id}}. */
    @GetMapping("/{id}")
    public TransactionResponse get(@RequestHeader(USER_ID_HEADER) String userId,
                                   @PathVariable("id") UUID id) {
        return transactionService.getTransaction(userId, id);
    }

    /**
     * {@code PATCH /v1/transactions/{id}} — partial update of editable fields
     * (description, reference, transactionDate). 422 if any immutable field
     * (accountId/entryType/amount/currency/source/categoryId/categoryName)
     * appears in the body.
     */
    @PatchMapping("/{id}")
    public TransactionResponse update(@RequestHeader(USER_ID_HEADER) String userId,
                                      @PathVariable("id") UUID id,
                                      @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.updateTransaction(userId, id, request);
    }

    /** Spec §3.2 — {@code PATCH /v1/transactions/{id}/category}. */
    @PatchMapping("/{id}/category")
    public CategorisedTransactionResponse categorise(@RequestHeader(USER_ID_HEADER) String userId,
                                                     @PathVariable("id") UUID id,
                                                     @Valid @RequestBody CategorisePatchRequest request) {
        return transactionService.categorise(userId, id, request);
    }

    /** Spec §3.2 — {@code DELETE /v1/transactions/{id}}. Returns 204. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(USER_ID_HEADER) String userId,
                       @PathVariable("id") UUID id) {
        transactionService.deleteTransaction(userId, id);
    }
}
