package com.personal.finance.transaction.repository.projection;

import java.util.UUID;

/**
 * One row per category-bucket from {@code GET /v1/transactions/counts}.
 * {@code categoryId} is {@code null} for the uncategorised bucket.
 */
public interface CategoryCountProjection {
    UUID getCategoryId();
    long getCount();
}
