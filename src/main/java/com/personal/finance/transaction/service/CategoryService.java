package com.personal.finance.transaction.service;

import com.personal.finance.transaction.dto.request.BulkCategoryItem;
import com.personal.finance.transaction.dto.request.CreateCategoryRequest;
import com.personal.finance.transaction.dto.request.UpdateCategoryRequest;
import com.personal.finance.transaction.dto.response.CategoryBulkResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.dto.response.CategorySummaryResponse;
import com.personal.finance.transaction.entity.Category;

import java.util.List;
import java.util.UUID;

/** Implements the category flows defined in spec §3.3. */
public interface CategoryService {

    /** Spec §3.3 POST /v1/categories. */
    CategoryResponse createCategory(String userId, CreateCategoryRequest request);

    /** Spec §3.3 GET /v1/categories. */
    List<CategoryResponse> listCategories(String userId);

    /** Spec §3.3 POST /v1/categories/bulk — idempotent upsert. */
    CategoryBulkResponse bulkUpsert(String userId, List<BulkCategoryItem> items);

    /** Spec §3.3 GET /v1/categories/{id}/summary. */
    CategorySummaryResponse summary(String userId, UUID categoryId);

    /** Spec §3.3 PUT /v1/categories/{id}. */
    CategoryResponse updateCategory(String userId, UUID categoryId, UpdateCategoryRequest request);

    /** Spec §3.3 DELETE /v1/categories/{id} — atomic double-write. */
    void deleteCategory(String userId, UUID categoryId);

    /**
     * Inline category resolution used by transaction-service. Returns the
     * existing row or inserts a new one. The {@code created} flag in
     * {@link ResolvedCategory} powers {@code isNew} in the PATCH response.
     */
    ResolvedCategory resolveByName(String userId, String name);

    /** Used by PATCH when {@code categoryId} is supplied. */
    Category loadOwnedById(String userId, UUID categoryId);

    /** Tuple-like return for {@link #resolveByName(String, String)}. */
    record ResolvedCategory(Category category, boolean created) {}
}
