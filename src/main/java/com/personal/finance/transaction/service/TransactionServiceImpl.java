package com.personal.finance.transaction.service;

import com.personal.finance.common.exception.ValidationException;
import com.personal.finance.transaction.client.account.AccountServiceClient;
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
import com.personal.finance.transaction.dto.response.CategoryRefResponse;
import com.personal.finance.transaction.dto.response.CountsResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;
import com.personal.finance.transaction.entity.Category;
import com.personal.finance.transaction.entity.Transaction;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import com.personal.finance.transaction.exception.ImmutableFieldUpdateException;
import com.personal.finance.transaction.exception.InvalidCategoryRequestException;
import com.personal.finance.transaction.exception.TransactionNotFoundException;
import com.personal.finance.transaction.mapper.TransactionMapper;
import com.personal.finance.transaction.repository.TransactionRepository;
import com.personal.finance.transaction.repository.projection.AccountBalanceAggregate;
import com.personal.finance.transaction.repository.projection.ActiveTransactionLookup;
import com.personal.finance.transaction.repository.projection.CategoryCountProjection;
import com.personal.finance.transaction.repository.specification.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final AccountServiceClient accountClient;
    private final TransactionMapper transactionMapper;

    /**
     * Spec §3.2 POST — High-Level Logic:
     * <ol>
     *   <li>Call Account Service → 404 / 422 on miss / closed.</li>
     *   <li>Resolve category by id (404 if missing) or by name (inline-create).</li>
     *   <li>INSERT with user_id, source=MANUAL.</li>
     *   <li>Return 201 with embedded category descriptor.</li>
     * </ol>
     */
    @Override
    @Transactional
    public TransactionResponse createTransaction(String userId, CreateTransactionRequest request) {
        accountClient.fetchActiveAccount(userId, request.getAccountId());

        CategoryResolution resolved = resolveCategoryForCreate(userId, request);

        Transaction entity = Transaction.builder()
                .userId(userId)
                .accountId(request.getAccountId())
                .categoryId(resolved.category == null ? null : resolved.category.getCategoryId())
                .entryType(request.getEntryType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionDate(request.getTransactionDate())
                .reference(request.getReference())
                .description(request.getDescription())
                .source(Source.MANUAL)
                .build();

        Transaction saved = transactionRepository.save(entity);
        log.info("Transaction created uid=[{}] id=[{}] account=[{}]",
                userId, saved.getTransactionId(), request.getAccountId());

        return transactionMapper.toResponse(saved, refOf(resolved));
    }

    /**
     * Spec §3.2 GET — paged list with optional filters. Categories are fetched
     * via a lookup map keyed on categoryId so we make one query per page
     * instead of N+1.
     *
     * <p>Filter mutual-exclusion: at most one of {@code categoryId},
     * {@code categoryIds}, or {@code uncategorized=true} may be set —
     * combining them is a 400 (the user's intent is ambiguous).
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponse listTransactions(String userId,
                                                    UUID accountId,
                                                    UUID categoryId,
                                                    Collection<UUID> categoryIds,
                                                    boolean uncategorized,
                                                    LocalDate from,
                                                    LocalDate to,
                                                    String q,
                                                    int page,
                                                    int size) {
        validateCategoryFilterExclusivity(categoryId, categoryIds, uncategorized);

        int effectivePage = Math.max(page - 1, 0); // spec is 1-indexed
        int effectiveSize = size <= 0 ? 50 : size;
        PageRequest pageable = PageRequest.of(effectivePage, effectiveSize,
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));

        Specification<Transaction> spec = TransactionSpecifications.activeForUserWithFilters(
                userId, accountId, categoryId, categoryIds, uncategorized, from, to, q);
        Page<Transaction> result = transactionRepository.findAll(spec, pageable);

        // Cheap category lookup: one map of categoryId → Category for any
        // categoryIds in the page. Soft-deleted categories return null → null ref.
        List<UUID> pageCategoryIds = result.getContent().stream()
                .map(Transaction::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<UUID, Category> categoryMap = new java.util.HashMap<>();
        for (UUID cid : pageCategoryIds) {
            try {
                categoryMap.put(cid, categoryService.loadOwnedById(userId, cid));
            } catch (Exception ignored) {
                // Category may have been soft-deleted concurrently — leave map entry absent.
            }
        }

        List<TransactionResponse> dtos = result.getContent().stream()
                .map(tx -> transactionMapper.toResponse(tx, refFor(tx, categoryMap)))
                .toList();

        return TransactionPageResponse.builder()
                .data(dtos)
                .total(result.getTotalElements())
                .page(page <= 0 ? 1 : page)
                .size(effectiveSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String userId, UUID transactionId) {
        Transaction tx = loadOwned(userId, transactionId);
        CategoryRefResponse ref = null;
        if (tx.getCategoryId() != null) {
            try {
                Category cat = categoryService.loadOwnedById(userId, tx.getCategoryId());
                ref = CategoryRefResponse.builder()
                        .id(cat.getCategoryId()).name(cat.getName()).isNew(false).build();
            } catch (Exception ignored) {
                // Category soft-deleted after this transaction was created — surface as null.
            }
        }
        return transactionMapper.toResponse(tx, ref);
    }

    /**
     * Spec §3.2 PATCH — accepts categoryId OR categoryName (exactly one).
     * Spec §2.4 inline-create logic delegated to {@code categoryService.resolveByName}.
     */
    @Override
    @Transactional
    public CategorisedTransactionResponse categorise(String userId, UUID transactionId,
                                                     CategorisePatchRequest request) {
        boolean hasId = request.getCategoryId() != null;
        boolean hasName = request.getCategoryName() != null && !request.getCategoryName().isBlank();
        if (hasId == hasName) {
            // Both or neither — spec §3.2 explicitly returns 400.
            throw new InvalidCategoryRequestException();
        }

        Transaction tx = loadOwned(userId, transactionId);

        Category category;
        boolean isNew;
        if (hasId) {
            category = categoryService.loadOwnedById(userId, request.getCategoryId());
            isNew = false;
        } else {
            CategoryService.ResolvedCategory resolved =
                    categoryService.resolveByName(userId, request.getCategoryName().trim());
            category = resolved.category();
            isNew = resolved.created();
        }

        tx.setCategoryId(category.getCategoryId());
        // tx is managed → save flushes on commit; explicit save here is fine
        // and makes the intent obvious to reviewers.
        Transaction saved = transactionRepository.save(tx);

        CategoryRefResponse ref = CategoryRefResponse.builder()
                .id(category.getCategoryId()).name(category.getName()).isNew(isNew).build();

        log.info("Transaction categorised uid=[{}] tx=[{}] cat=[{}] isNew=[{}]",
                userId, transactionId, category.getCategoryId(), isNew);

        return CategorisedTransactionResponse.builder()
                .transaction(transactionMapper.toResponse(saved, ref))
                .category(ref)
                .build();
    }

    /**
     * {@code PATCH /v1/transactions/{id}} — partial update. Editable fields
     * are applied only if non-null; immutable fields appearing in the body
     * trigger a 422 before any mutation. Category changes still go through
     * {@code PATCH /{id}/category} (whose inline-create flow doesn't
     * generalise to a plain update).
     */
    @Override
    @Transactional
    public TransactionResponse updateTransaction(String userId, UUID transactionId,
                                                 UpdateTransactionRequest request) {
        rejectImmutableFields(request);

        Transaction tx = loadOwned(userId, transactionId);
        if (request.getDescription() != null) {
            tx.setDescription(request.getDescription());
        }
        if (request.getReference() != null) {
            tx.setReference(request.getReference());
        }
        if (request.getTransactionDate() != null) {
            tx.setTransactionDate(request.getTransactionDate());
        }
        Transaction saved = transactionRepository.save(tx);

        log.info("Transaction updated uid=[{}] id=[{}]", userId, transactionId);
        return transactionMapper.toResponse(saved, currentCategoryRef(userId, saved));
    }

    @Override
    @Transactional
    public void deleteTransaction(String userId, UUID transactionId) {
        int updated = transactionRepository.softDelete(transactionId, userId, OffsetDateTime.now());
        if (updated == 0) {
            throw new TransactionNotFoundException(
                    "Transaction " + transactionId + " not found, already deleted, or not owned");
        }
        log.info("Transaction soft-deleted uid=[{}] id=[{}]", userId, transactionId);
    }

    /**
     * {@code GET /v1/transactions/balances} — single GROUP BY aggregate.
     * {@code asOf} defaults to today when null. An empty (but non-null)
     * {@code accountIds} collection short-circuits to an empty result rather
     * than firing an {@code IN ()} that some JPA providers reject.
     */
    @Override
    @Transactional(readOnly = true)
    public BalancesResponse listBalances(String userId, LocalDate asOf, Collection<UUID> accountIds) {
        LocalDate effectiveAsOf = asOf == null ? LocalDate.now() : asOf;

        if (accountIds != null && accountIds.isEmpty()) {
            return BalancesResponse.builder()
                    .asOf(effectiveAsOf)
                    .balances(List.of())
                    .build();
        }

        List<AccountBalanceAggregate> rows = (accountIds == null)
                ? transactionRepository.aggregateBalances(
                        userId, effectiveAsOf, EntryType.CREDIT, EntryType.DEBIT)
                : transactionRepository.aggregateBalancesForAccounts(
                        userId, effectiveAsOf, accountIds, EntryType.CREDIT, EntryType.DEBIT);

        List<BalancesResponse.AccountBalance> balances = rows.stream()
                .map(this::toAccountBalance)
                .toList();

        return BalancesResponse.builder()
                .asOf(effectiveAsOf)
                .balances(balances)
                .build();
    }

    /**
     * {@code PATCH /v1/transactions/bulk-category} — resolve the target
     * category once (inline-create if a name was given), then split the input
     * ids into updated / skipped / notFound buckets before issuing a single
     * bulk UPDATE. Honest reporting: a row already in the target category
     * counts as {@code skipped}, not {@code updated}.
     */
    @Override
    @Transactional
    public BulkCategoryResponse bulkSetCategory(String userId, BulkCategoryRequest request) {
        boolean hasId = request.getCategoryId() != null;
        boolean hasName = request.getCategoryName() != null && !request.getCategoryName().isBlank();
        if (hasId == hasName) {
            throw new InvalidCategoryRequestException();
        }

        Category category;
        boolean isNew;
        if (hasId) {
            category = categoryService.loadOwnedById(userId, request.getCategoryId());
            isNew = false;
        } else {
            CategoryService.ResolvedCategory resolved =
                    categoryService.resolveByName(userId, request.getCategoryName().trim());
            category = resolved.category();
            isNew = resolved.created();
        }

        List<UUID> requested = request.getTransactionIds();
        List<ActiveTransactionLookup> found = transactionRepository.findActiveLookupByIds(userId, requested);

        java.util.Set<UUID> foundIds = new java.util.HashSet<>(found.size());
        List<UUID> toUpdate = new ArrayList<>();
        int skipped = 0;
        for (ActiveTransactionLookup row : found) {
            foundIds.add(row.getTransactionId());
            if (category.getCategoryId().equals(row.getCategoryId())) {
                skipped++;
            } else {
                toUpdate.add(row.getTransactionId());
            }
        }

        List<UUID> notFound = requested.stream().filter(id -> !foundIds.contains(id)).toList();

        int updated = 0;
        if (!toUpdate.isEmpty()) {
            updated = transactionRepository.bulkSetCategory(toUpdate, userId, category.getCategoryId());
        }

        log.info("Bulk categorise uid=[{}] cat=[{}] requested=[{}] updated=[{}] skipped=[{}] notFound=[{}]",
                userId, category.getCategoryId(), requested.size(), updated, skipped, notFound.size());

        return BulkCategoryResponse.builder()
                .updated(updated)
                .skipped(skipped)
                .notFound(notFound)
                .category(CategoryRefResponse.builder()
                        .id(category.getCategoryId()).name(category.getName()).isNew(isNew).build())
                .build();
    }

    /**
     * {@code DELETE /v1/transactions/bulk} — pre-SELECT to know which ids
     * exist and are owned, then soft-delete them in a single UPDATE. {@code
     * notFound} covers ids that don't exist, were already soft-deleted, or
     * aren't owned by this user.
     */
    @Override
    @Transactional
    public BulkDeleteResponse bulkDelete(String userId, BulkDeleteRequest request) {
        List<UUID> requested = request.getTransactionIds();
        List<ActiveTransactionLookup> found = transactionRepository.findActiveLookupByIds(userId, requested);

        List<UUID> foundIds = found.stream().map(ActiveTransactionLookup::getTransactionId).toList();
        java.util.Set<UUID> foundSet = new java.util.HashSet<>(foundIds);
        List<UUID> notFound = requested.stream().filter(id -> !foundSet.contains(id)).toList();

        int deleted = 0;
        if (!foundIds.isEmpty()) {
            deleted = transactionRepository.bulkSoftDelete(foundIds, userId, OffsetDateTime.now());
        }

        log.info("Bulk delete uid=[{}] requested=[{}] deleted=[{}] notFound=[{}]",
                userId, requested.size(), deleted, notFound.size());

        return BulkDeleteResponse.builder()
                .deleted(deleted)
                .notFound(notFound)
                .build();
    }

    /**
     * Spec §3.2 POST /batch — per-row isolation: validation failures collected
     * into failedRows, valid rows committed.
     */
    @Override
    public BatchInsertResponse insertBatch(String userId, BatchTransactionsRequest request) {
        List<BatchInsertResponse.FailedRow> failed = new ArrayList<>();
        int inserted = 0;
        for (int i = 0; i < request.getRows().size(); i++) {
            BatchTransactionsRequest.Row row = request.getRows().get(i);
            try {
                persistBatchRow(userId, request.getBulkJobId(), row);
                inserted++;
            } catch (Exception ex) {
                log.warn("Batch row {} failed: {}", i, ex.getMessage());
                failed.add(BatchInsertResponse.FailedRow.builder()
                        .rowIndex(i).reason(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
                        .build());
            }
        }
        log.info("Bulk batch uid=[{}] job=[{}] inserted=[{}] failed=[{}]",
                userId, request.getBulkJobId(), inserted, failed.size());
        return BatchInsertResponse.builder().insertedCount(inserted).failedRows(failed).build();
    }

    /**
     * Each row is committed in its own transaction so a single failure does
     * not poison the rest of the batch. Spec §3.2 batch endpoint: "Successful
     * rows are committed. Failures are not — per-row isolation."
     */
    @Transactional
    protected void persistBatchRow(String userId, UUID bulkJobId, BatchTransactionsRequest.Row row) {
        UUID categoryId = resolveBatchRowCategory(userId, row);
        Transaction entity = Transaction.builder()
                .userId(userId)
                .accountId(row.getAccountId())
                .categoryId(categoryId)
                .entryType(row.getEntryType())
                .amount(row.getAmount())
                .currency(row.getCurrency())
                .transactionDate(row.getTransactionDate())
                .reference(row.getReference())
                .description(row.getDescription())
                .source(Source.BULK)
                .bulkJobId(bulkJobId)
                .build();
        transactionRepository.save(entity);
    }

    private UUID resolveBatchRowCategory(String userId, BatchTransactionsRequest.Row row) {
        boolean hasId = row.getCategoryId() != null;
        boolean hasName = row.getCategoryName() != null && !row.getCategoryName().isBlank();
        if (hasId && hasName) {
            throw new InvalidCategoryRequestException();
        }
        if (hasId) {
            return categoryService.loadOwnedById(userId, row.getCategoryId()).getCategoryId();
        }
        if (hasName) {
            return categoryService.resolveByName(userId, row.getCategoryName().trim()).category().getCategoryId();
        }
        return null;
    }

    /**
     * {@code GET /v1/transactions/counts} — single GROUP BY in the repository,
     * folded in memory. The {@code null}-key row is the uncategorised bucket;
     * the rest land in {@code byCategory} and add up (plus uncategorised) to
     * {@code total}. Empty {@code byCategory} when the user has no
     * categorised rows yet.
     */
    @Override
    @Transactional(readOnly = true)
    public CountsResponse listCounts(String userId) {
        List<CategoryCountProjection> rows = transactionRepository.countActiveGroupedByCategory(userId);

        long uncategorized = 0L;
        long total = 0L;
        Map<UUID, Long> byCategory = new LinkedHashMap<>();
        for (CategoryCountProjection row : rows) {
            long count = row.getCount();
            total += count;
            if (row.getCategoryId() == null) {
                uncategorized = count;
            } else {
                byCategory.put(row.getCategoryId(), count);
            }
        }

        return CountsResponse.builder()
                .total(total)
                .uncategorized(uncategorized)
                .byCategory(byCategory)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Reject combinations of the three category filters — they have
     * incompatible semantics and silently choosing one would hide bugs in the
     * caller. Empty {@code categoryIds} counts as "not set".
     */
    private void validateCategoryFilterExclusivity(UUID categoryId,
                                                   Collection<UUID> categoryIds,
                                                   boolean uncategorized) {
        int set = 0;
        if (categoryId != null) set++;
        if (categoryIds != null && !categoryIds.isEmpty()) set++;
        if (uncategorized) set++;
        if (set > 1) {
            throw new ValidationException("categoryFilter",
                    "at most one of categoryId, categoryIds, uncategorized=true may be provided");
        }
    }

    /**
     * Reject any non-null immutable field — accountId/entryType/amount/
     * currency/source per double-entry hygiene, plus categoryId/categoryName
     * because category changes have their own endpoint with inline-create
     * semantics that don't generalise.
     */
    private void rejectImmutableFields(UpdateTransactionRequest request) {
        List<String> attempted = new ArrayList<>();
        if (request.getAccountId() != null) attempted.add("accountId");
        if (request.getEntryType() != null) attempted.add("entryType");
        if (request.getAmount() != null) attempted.add("amount");
        if (request.getCurrency() != null) attempted.add("currency");
        if (request.getSource() != null) attempted.add("source");
        if (request.getCategoryId() != null) attempted.add("categoryId");
        if (request.getCategoryName() != null) attempted.add("categoryName");
        if (!attempted.isEmpty()) {
            throw new ImmutableFieldUpdateException(attempted);
        }
    }

    /** Re-resolves the category ref for a saved transaction; soft-deleted → null. */
    private CategoryRefResponse currentCategoryRef(String userId, Transaction tx) {
        if (tx.getCategoryId() == null) return null;
        try {
            Category cat = categoryService.loadOwnedById(userId, tx.getCategoryId());
            return CategoryRefResponse.builder()
                    .id(cat.getCategoryId()).name(cat.getName()).isNew(false).build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Transaction loadOwned(String userId, UUID id) {
        return transactionRepository.findActiveByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction " + id + " not found for user " + userId));
    }

    private CategoryResolution resolveCategoryForCreate(String userId, CreateTransactionRequest request) {
        if (request.getCategoryId() != null) {
            Category cat = categoryService.loadOwnedById(userId, request.getCategoryId());
            return new CategoryResolution(cat, false);
        }
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            CategoryService.ResolvedCategory resolved =
                    categoryService.resolveByName(userId, request.getCategoryName().trim());
            return new CategoryResolution(resolved.category(), resolved.created());
        }
        return new CategoryResolution(null, false);
    }

    private CategoryRefResponse refOf(CategoryResolution resolved) {
        if (resolved.category == null) return null;
        return CategoryRefResponse.builder()
                .id(resolved.category.getCategoryId())
                .name(resolved.category.getName())
                .isNew(resolved.isNew)
                .build();
    }

    private BalancesResponse.AccountBalance toAccountBalance(AccountBalanceAggregate row) {
        return BalancesResponse.AccountBalance.builder()
                .accountId(row.getAccountId())
                .currency(row.getCurrency())
                .totalCredit(row.getTotalCredit())
                .totalDebit(row.getTotalDebit())
                .balance(row.getTotalCredit().subtract(row.getTotalDebit()))
                .txnCount(row.getTxnCount())
                .lastTxnDate(row.getLastTxnDate())
                .build();
    }

    private CategoryRefResponse refFor(Transaction tx, java.util.Map<UUID, Category> map) {
        if (tx.getCategoryId() == null) return null;
        Category cat = map.get(tx.getCategoryId());
        if (cat == null) return null;
        return CategoryRefResponse.builder()
                .id(cat.getCategoryId()).name(cat.getName()).isNew(false).build();
    }

    private record CategoryResolution(Category category, boolean isNew) {}
}
