package com.personal.finance.transaction.service;

import com.personal.finance.transaction.dto.request.BulkCategoryItem;
import com.personal.finance.transaction.dto.request.CreateCategoryRequest;
import com.personal.finance.transaction.dto.request.UpdateCategoryRequest;
import com.personal.finance.transaction.dto.response.CategoryBulkResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.dto.response.CategorySummaryResponse;
import com.personal.finance.transaction.entity.Category;
import com.personal.finance.transaction.exception.CategoryNameConflictException;
import com.personal.finance.transaction.exception.CategoryNotFoundException;
import com.personal.finance.transaction.mapper.CategoryMapper;
import com.personal.finance.transaction.repository.CategoryRepository;
import com.personal.finance.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Spec §3.3 POST — High-Level Logic:
     * <ol>
     *   <li>Check UNIQUE (user_id, name) WHERE deleted_at IS NULL. 409 if conflict.</li>
     *   <li>INSERT into categories.</li>
     *   <li>Return 201.</li>
     * </ol>
     * Production note: the partial unique index in spec §1.4 is the
     * authoritative guard against races; the SELECT-then-INSERT here is the
     * dev-mode equivalent since {@code ddl-auto=update} cannot create that
     * index (see TODO on {@link Category}).
     */
    @Override
    @Transactional
    public CategoryResponse createCategory(String userId, CreateCategoryRequest request) {
        categoryRepository.findActiveByNameAndUserId(request.getName(), userId)
                .ifPresent(c -> { throw new CategoryNameConflictException(request.getName()); });

        Category entity = Category.builder()
                .userId(userId)
                .name(request.getName())
                .colourHex(request.getColourHex())
                .build();
        Category saved = categoryRepository.save(entity);
        log.info("Category created uid=[{}] id=[{}] name=[{}]", userId, saved.getCategoryId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    /** Spec §3.3 GET — active categories for the user, sorted by name. */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(String userId) {
        return categoryRepository.findActiveByUserId(userId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Spec §3.3 POST /bulk — idempotent upsert. Implemented as a single batch:
     * <ol>
     *   <li>Fetch every active row whose name appears in the request.</li>
     *   <li>Existing → skipped++ (note duplicate name within request is also skipped).</li>
     *   <li>Missing → INSERT, created++.</li>
     *   <li>Return aggregate counts + the (categoryId, name) pairs for all
     *       categories the user now owns by these names.</li>
     * </ol>
     * Spec calls for {@code ON CONFLICT DO NOTHING} natively; this equivalent
     * keeps the contract (counts + final id list) without needing a Postgres
     * partial index at dev time. The race window between SELECT and INSERT is
     * the same one the partial index would close in production.
     */
    @Override
    @Transactional
    public CategoryBulkResponse bulkUpsert(String userId, List<BulkCategoryItem> items) {
        Set<String> distinctNames = new HashSet<>();
        List<BulkCategoryItem> distinct = new ArrayList<>();
        for (BulkCategoryItem item : items) {
            if (distinctNames.add(item.getName())) {
                distinct.add(item);
            }
        }

        Map<String, Category> existing = categoryRepository
                .findActiveByUserIdAndNameIn(userId, distinctNames)
                .stream()
                .collect(Collectors.toMap(Category::getName, c -> c));

        int created = 0;
        int skipped = items.size() - distinct.size(); // duplicates within request
        List<CategoryBulkResponse.Item> resultItems = new ArrayList<>(distinct.size());

        for (BulkCategoryItem item : distinct) {
            Category cat = existing.get(item.getName());
            if (cat == null) {
                cat = categoryRepository.save(Category.builder()
                        .userId(userId)
                        .name(item.getName())
                        .colourHex(item.getColourHex())
                        .build());
                created++;
            } else {
                skipped++;
            }
            resultItems.add(CategoryBulkResponse.Item.builder()
                    .categoryId(cat.getCategoryId())
                    .name(cat.getName())
                    .build());
        }
        log.info("Bulk category upsert uid=[{}] created=[{}] skipped=[{}]", userId, created, skipped);
        return CategoryBulkResponse.builder()
                .created(created)
                .skipped(skipped)
                .categories(resultItems)
                .build();
    }

    /**
     * Spec §3.3 GET /summary — lightweight aggregate for the delete dialog.
     * Two cheap queries: COUNT + SUM, plus one sampled currency.
     */
    @Override
    @Transactional(readOnly = true)
    public CategorySummaryResponse summary(String userId, UUID categoryId) {
        Category cat = loadOwnedById(userId, categoryId);
        long count = transactionRepository.countActiveByCategory(categoryId, userId);
        BigDecimal total = transactionRepository.sumActiveByCategory(categoryId, userId);
        String currency = count > 0 ? transactionRepository.pickCurrencyForCategory(categoryId, userId) : null;
        return CategorySummaryResponse.builder()
                .categoryId(cat.getCategoryId())
                .categoryName(cat.getName())
                .transactionCount(count)
                .totalAmount(total == null ? BigDecimal.ZERO : total)
                .currency(currency)
                .build();
    }

    /**
     * Spec §3.3 PUT — rename / recolour. Name conflict check is scoped to
     * other active rows owned by the user.
     */
    @Override
    @Transactional
    public CategoryResponse updateCategory(String userId, UUID categoryId, UpdateCategoryRequest request) {
        Category cat = loadOwnedById(userId, categoryId);
        if (request.getName() != null && !request.getName().equals(cat.getName())) {
            Optional<Category> conflict = categoryRepository.findActiveByNameAndUserId(request.getName(), userId);
            if (conflict.isPresent() && !conflict.get().getCategoryId().equals(categoryId)) {
                throw new CategoryNameConflictException(request.getName());
            }
            cat.setName(request.getName());
        }
        if (request.getColourHex() != null) {
            cat.setColourHex(request.getColourHex());
        }
        log.info("Category updated uid=[{}] id=[{}]", userId, categoryId);
        return categoryMapper.toResponse(cat);
    }

    /**
     * Spec §3.3 DELETE — atomic double-write per the prompt's "Specific
     * Behaviours" section. Both writes happen in this single transaction; if
     * either fails Spring rolls back so transactions are never orphaned to a
     * deleted category and the category never appears half-deleted.
     */
    @Override
    @Transactional
    public void deleteCategory(String userId, UUID categoryId) {
        // 1. Verify the row exists and is owned, before doing any writes.
        loadOwnedById(userId, categoryId);

        OffsetDateTime now = OffsetDateTime.now();

        // WRITE 1 — soft-delete the category.
        int deleted = categoryRepository.softDelete(categoryId, userId, now);
        if (deleted == 0) {
            // Race: another caller deleted it between our SELECT and UPDATE.
            throw new CategoryNotFoundException("Category " + categoryId + " already deleted");
        }

        // WRITE 2 — clear category_id on every linked active transaction.
        int cleared = transactionRepository.clearCategory(categoryId, userId);
        log.info("Category deleted uid=[{}] id=[{}] clearedTx=[{}]", userId, categoryId, cleared);
    }

    /**
     * Used by transaction-service for {@code PATCH /v1/transactions/{id}/category}
     * when {@code categoryName} is supplied. Implements spec §2.4 / §3.2 inline
     * create.
     */
    @Override
    @Transactional
    public ResolvedCategory resolveByName(String userId, String name) {
        Optional<Category> existing = categoryRepository.findActiveByNameAndUserId(name, userId);
        if (existing.isPresent()) {
            return new ResolvedCategory(existing.get(), false);
        }
        Category created = categoryRepository.save(Category.builder()
                .userId(userId)
                .name(name)
                .build());
        log.info("Inline category created uid=[{}] id=[{}] name=[{}]",
                userId, created.getCategoryId(), name);
        return new ResolvedCategory(created, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Category loadOwnedById(String userId, UUID categoryId) {
        return categoryRepository.findActiveByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category " + categoryId + " not found for user " + userId));
    }
}
