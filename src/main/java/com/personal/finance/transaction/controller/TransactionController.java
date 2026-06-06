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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Transactions", description = "Create, query, categorise and delete financial transactions for authenticated users.")
public class TransactionController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final TransactionService transactionService;

    /** Spec §3.2 — {@code POST /v1/transactions}. Returns 201. */
    @Operation(
            summary = "Create a transaction",
            description = "Creates a new DEBIT or CREDIT transaction for the authenticated user. "
                    + "Supply either categoryId (existing category) or categoryName (inline create) — not both. "
                    + "Returns 404 when the account does not exist or is not owned by the user, "
                    + "422 when the account is CLOSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — missing/invalid fields",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found or not owned by user",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "422", description = "Account is CLOSED",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
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
    @Operation(
            summary = "List transactions",
            description = "Returns a paginated list of active transactions for the authenticated user. "
                    + "Optional filters: accountId, date range (from/to), category (categoryId, categoryIds, or uncategorized=true — mutually exclusive), "
                    + "and a free-text search query (q) matched case-insensitively against description and reference. "
                    + "Returns 400 if more than one category filter type is supplied simultaneously.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated transaction list",
                    content = @Content(schema = @Schema(implementation = TransactionPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Conflicting category filter parameters",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping
    public TransactionPageResponse list(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Filter by account UUID", schema = @Schema(type = "string", format = "uuid"))
            @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Filter by a single category UUID (mutually exclusive with categoryIds and uncategorized)",
                    schema = @Schema(type = "string", format = "uuid"))
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter by multiple category UUIDs (mutually exclusive with categoryId and uncategorized)")
            @RequestParam(required = false) List<UUID> categoryIds,
            @Parameter(description = "Return only uncategorised transactions (mutually exclusive with categoryId and categoryIds)",
                    schema = @Schema(type = "boolean", defaultValue = "false"))
            @RequestParam(required = false, defaultValue = "false") boolean uncategorized,
            @Parameter(description = "Start date (inclusive), ISO-8601 (yyyy-MM-dd)",
                    schema = @Schema(type = "string", format = "date", example = "2024-01-01"))
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive), ISO-8601 (yyyy-MM-dd)",
                    schema = @Schema(type = "string", format = "date", example = "2024-12-31"))
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Free-text search query matched against description and reference (case-insensitive LIKE)",
                    schema = @Schema(type = "string", example = "grocery"))
            @RequestParam(required = false) String q,
            @Parameter(description = "1-based page number", schema = @Schema(type = "integer", defaultValue = "1", example = "1"))
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "Page size (max enforced by service)", schema = @Schema(type = "integer", defaultValue = "50", example = "50"))
            @RequestParam(required = false, defaultValue = "50") int size) {
        return transactionService.listTransactions(
                userId, accountId, categoryId, categoryIds, uncategorized, from, to, q, page, size);
    }

    /**
     * Spec §3.2 — {@code POST /v1/transactions/batch} (internal).
     * Declared BEFORE {@code /{id}} so Spring picks the static path first.
     */
    @Operation(
            summary = "Batch-insert transactions (internal)",
            description = "Inserts up to 500 transaction rows originating from a bulk-upload job. "
                    + "Intended for service-to-service calls from the ingestion service. "
                    + "Rows that fail validation or reference a missing/closed account are recorded in failedRows without aborting the batch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch processed — inspect insertedCount and failedRows",
                    content = @Content(schema = @Schema(implementation = BatchInsertResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request-level validation failure (e.g. empty rows list)",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PostMapping("/batch")
    public BatchInsertResponse batch(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody BatchTransactionsRequest request) {
        return transactionService.insertBatch(userId, request);
    }

    /**
     * {@code GET /v1/transactions/counts} — total / uncategorised /
     * per-category active transaction counts. Declared BEFORE {@code /{id}}
     * so Spring routes the literal path first.
     */
    @Operation(
            summary = "Get transaction counts",
            description = "Returns the total number of active transactions, how many are uncategorised, "
                    + "and a per-category breakdown. Intended for sidebar badges and filter chips in the UI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction counts",
                    content = @Content(schema = @Schema(implementation = CountsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping("/counts")
    public CountsResponse counts(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId) {
        return transactionService.listCounts(userId);
    }

    /**
     * {@code GET /v1/transactions/balances} — per-account aggregates.
     * Declared BEFORE {@code /{id}} so Spring routes the literal path first.
     */
    @Operation(
            summary = "Get account balances",
            description = "Returns per-account credit/debit aggregates computed from active transactions "
                    + "dated on or before asOf (defaults to today). Does not include the opening balance held "
                    + "by the account service — callers add that separately.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-account balance aggregates",
                    content = @Content(schema = @Schema(implementation = BalancesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping("/balances")
    public BalancesResponse balances(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Aggregate transactions up to and including this date (defaults to today), ISO-8601",
                    schema = @Schema(type = "string", format = "date", example = "2024-12-31"))
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @Parameter(description = "Restrict to these account UUIDs; omit to return all accounts")
            @RequestParam(required = false) List<UUID> accountIds) {
        return transactionService.listBalances(userId, asOf, accountIds);
    }

    /** {@code PATCH /v1/transactions/bulk-category} — atomic bulk re-categorise. */
    @Operation(
            summary = "Bulk re-categorise transactions",
            description = "Atomically assigns a category to up to 500 transactions. "
                    + "Supply either categoryId (existing) or categoryName (inline create) — not both. "
                    + "Transactions that are not found, already deleted, or not owned by the user are reported in notFound without failing the whole request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk categorisation result",
                    content = @Content(schema = @Schema(implementation = BulkCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or conflicting category inputs",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PatchMapping("/bulk-category")
    public BulkCategoryResponse bulkCategory(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody BulkCategoryRequest request) {
        return transactionService.bulkSetCategory(userId, request);
    }

    /** {@code DELETE /v1/transactions/bulk} — atomic bulk soft-delete. */
    @Operation(
            summary = "Bulk soft-delete transactions",
            description = "Soft-deletes up to 500 transactions in a single atomic operation. "
                    + "Transactions that are not found, already deleted, or not owned by the user are reported in notFound without failing the whole request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk delete result",
                    content = @Content(schema = @Schema(implementation = BulkDeleteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — empty or oversized id list",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @DeleteMapping("/bulk")
    public BulkDeleteResponse bulkDelete(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody BulkDeleteRequest request) {
        return transactionService.bulkDelete(userId, request);
    }

    /** Spec §3.2 — {@code GET /v1/transactions/{id}}. */
    @Operation(
            summary = "Get a transaction",
            description = "Retrieves a single active transaction by its UUID. Returns 404 if the transaction "
                    + "does not exist, has been soft-deleted, or is not owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public TransactionResponse get(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Transaction UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            @PathVariable("id") UUID id) {
        return transactionService.getTransaction(userId, id);
    }

    /**
     * {@code PATCH /v1/transactions/{id}} — partial update of editable fields
     * (description, reference, transactionDate). 422 if any immutable field
     * (accountId/entryType/amount/currency/source/categoryId/categoryName)
     * appears in the body.
     */
    @Operation(
            summary = "Partially update a transaction",
            description = "Updates editable fields — description, reference, transactionDate — on a single transaction. "
                    + "Only non-null supplied fields are applied. Returns 422 (UNPROCESSABLE_ENTITY) if any immutable "
                    + "field (accountId, entryType, amount, currency, source, categoryId, categoryName) is present in the request body; "
                    + "use DELETE + POST to recreate with different core attributes. Category changes use PATCH /{id}/category instead.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "422", description = "Attempt to mutate an immutable field",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PatchMapping("/{id}")
    public TransactionResponse update(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Transaction UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.updateTransaction(userId, id, request);
    }

    /** Spec §3.2 — {@code PATCH /v1/transactions/{id}/category}. */
    @Operation(
            summary = "Assign or create a category for a transaction",
            description = "Sets the category on a single transaction. Supply exactly one of categoryId (existing) "
                    + "or categoryName (inline create — creates the category if it does not exist). "
                    + "Returns 400 when both or neither are provided. Returns 404 when the transaction or the supplied categoryId does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction categorised; isNew=true when the category was created inline",
                    content = @Content(schema = @Schema(implementation = CategorisedTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Both or neither of categoryId / categoryName provided",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction or categoryId not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PatchMapping("/{id}/category")
    public CategorisedTransactionResponse categorise(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Transaction UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            @PathVariable("id") UUID id,
            @Valid @RequestBody CategorisePatchRequest request) {
        return transactionService.categorise(userId, id, request);
    }

    /** Spec §3.2 — {@code DELETE /v1/transactions/{id}}. Returns 204. */
    @Operation(
            summary = "Soft-delete a transaction",
            description = "Marks a single transaction as deleted. The row is retained in the database for audit purposes but excluded from all list and aggregate queries. "
                    + "Returns 404 if the transaction does not exist, is already deleted, or is not owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted — no body"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Transaction UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            @PathVariable("id") UUID id) {
        transactionService.deleteTransaction(userId, id);
    }
}
