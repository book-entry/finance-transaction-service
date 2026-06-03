package com.personal.finance.transaction.repository.projection;

import java.util.UUID;

/**
 * Lightweight per-row projection for bulk endpoints — returns only the
 * {@code transactionId} and current {@code categoryId} of active, owned
 * transactions so the service can split inputs into updated / skipped /
 * notFound buckets without loading full entities.
 */
public interface ActiveTransactionLookup {
    UUID getTransactionId();
    /** {@code null} when the row is currently uncategorised. */
    UUID getCategoryId();
}
