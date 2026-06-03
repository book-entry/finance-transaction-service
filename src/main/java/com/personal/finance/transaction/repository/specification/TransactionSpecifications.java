package com.personal.finance.transaction.repository.specification;

import com.personal.finance.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spec builder for the filtered list endpoint. Kept as a single
 * {@link Specification} factory rather than per-predicate methods so all WHERE
 * clauses are visible in one place — the existing {@code findActiveWithFilters}
 * {@code @Query} accepted only the original four filters and adding three more
 * (q, uncategorized, categoryIds) as sentinel parameters would hurt
 * readability more than the criteria API does.
 *
 * <p>Category filter ordering — {@code categoryId} > {@code categoryIds} >
 * {@code uncategorized}. The controller / service is responsible for rejecting
 * combinations; this builder only applies whichever is non-null.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> activeForUserWithFilters(
            String userId,
            UUID accountId,
            UUID categoryId,
            Collection<UUID> categoryIds,
            boolean uncategorized,
            LocalDate from,
            LocalDate to,
            String q) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("userId"), userId));
            preds.add(cb.isNull(root.get("deletedAt")));

            if (accountId != null) {
                preds.add(cb.equal(root.get("accountId"), accountId));
            }
            if (categoryId != null) {
                preds.add(cb.equal(root.get("categoryId"), categoryId));
            } else if (categoryIds != null && !categoryIds.isEmpty()) {
                preds.add(root.get("categoryId").in(categoryIds));
            } else if (uncategorized) {
                preds.add(cb.isNull(root.get("categoryId")));
            }
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("reference")), pattern)));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
