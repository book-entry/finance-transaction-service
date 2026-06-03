package com.personal.finance.transaction.repository;

import com.personal.finance.transaction.entity.Transaction;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.repository.projection.AccountBalanceAggregate;
import com.personal.finance.transaction.repository.projection.ActiveTransactionLookup;
import com.personal.finance.transaction.repository.projection.CategoryCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction persistence — every finder filters {@code deleted_at IS NULL}
 * per spec §1.3. {@link JpaSpecificationExecutor} powers the optional-filter
 * list endpoint without an explosion of derived query methods.
 */
@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :id AND t.userId = :userId AND t.deletedAt IS NULL")
    Optional<Transaction> findActiveByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    /** Spec §3.2 DELETE — sets deleted_at; only rows currently active. */
    @Modifying
    @Query("UPDATE Transaction t SET t.deletedAt = :now WHERE t.transactionId = :id AND t.userId = :userId AND t.deletedAt IS NULL")
    int softDelete(@Param("id") UUID id, @Param("userId") String userId, @Param("now") OffsetDateTime now);

    /** Spec §3.2 PATCH — assign / re-assign category. */
    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = :categoryId WHERE t.transactionId = :id AND t.userId = :userId AND t.deletedAt IS NULL")
    int setCategory(@Param("id") UUID id, @Param("userId") String userId, @Param("categoryId") UUID categoryId);

    /**
     * Per-id lookup for the bulk endpoints — returns one row per active+owned
     * transaction so the service can compute updated / skipped / notFound
     * buckets in memory before issuing the write.
     */
    @Query("SELECT t.transactionId AS transactionId, t.categoryId AS categoryId "
            + "FROM Transaction t WHERE t.userId = :userId AND t.deletedAt IS NULL "
            + "AND t.transactionId IN :ids")
    List<ActiveTransactionLookup> findActiveLookupByIds(
            @Param("userId") String userId,
            @Param("ids") Collection<UUID> ids);

    /** Bulk re-categorise — used by {@code PATCH /v1/transactions/bulk-category}. */
    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = :categoryId "
            + "WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.transactionId IN :ids")
    int bulkSetCategory(
            @Param("ids") Collection<UUID> ids,
            @Param("userId") String userId,
            @Param("categoryId") UUID categoryId);

    /** Bulk soft-delete — used by {@code DELETE /v1/transactions/bulk}. */
    @Modifying
    @Query("UPDATE Transaction t SET t.deletedAt = :now "
            + "WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.transactionId IN :ids")
    int bulkSoftDelete(
            @Param("ids") Collection<UUID> ids,
            @Param("userId") String userId,
            @Param("now") OffsetDateTime now);

    /**
     * Atomic-delete WRITE 2 — clears category_id on every active transaction
     * linked to the deleted category. Spec §3.3 DELETE.
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = NULL WHERE t.categoryId = :categoryId AND t.userId = :userId AND t.deletedAt IS NULL")
    int clearCategory(@Param("categoryId") UUID categoryId, @Param("userId") String userId);

    /** Spec §3.3 GET /categories/{id}/summary — count of active transactions. */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.categoryId = :categoryId AND t.userId = :userId AND t.deletedAt IS NULL")
    long countActiveByCategory(@Param("categoryId") UUID categoryId, @Param("userId") String userId);

    /** Spec §3.3 GET /categories/{id}/summary — sum of active amounts. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.categoryId = :categoryId AND t.userId = :userId AND t.deletedAt IS NULL")
    BigDecimal sumActiveByCategory(@Param("categoryId") UUID categoryId, @Param("userId") String userId);

    /** Sample currency from the first matching transaction — null if none. */
    @Query("SELECT t.currency FROM Transaction t WHERE t.categoryId = :categoryId AND t.userId = :userId AND t.deletedAt IS NULL ORDER BY t.createdAt ASC LIMIT 1")
    String pickCurrencyForCategory(@Param("categoryId") UUID categoryId, @Param("userId") String userId);

    /**
     * Per-category active transaction count for {@code GET
     * /v1/transactions/counts}. Single GROUP BY — the {@code null} group
     * surfaces as the uncategorised bucket; the service sums the rest into
     * {@code total} and folds the {@code null} row into {@code uncategorized}.
     */
    @Query("SELECT t.categoryId AS categoryId, COUNT(t) AS count "
            + "FROM Transaction t WHERE t.userId = :userId AND t.deletedAt IS NULL "
            + "GROUP BY t.categoryId")
    List<CategoryCountProjection> countActiveGroupedByCategory(@Param("userId") String userId);

    /**
     * Per-account aggregates for {@code GET /v1/transactions/balances}.
     * Grouped by {@code (accountId, currency)} so mixed-currency data on one
     * account surfaces rather than being silently summed across currencies.
     * Two methods (with / without the {@code accountIds} filter) keep the JPQL
     * straightforward — passing an empty {@code IN} list to Hibernate can throw.
     */
    String BALANCE_AGG_SELECT =
            "SELECT t.accountId AS accountId, t.currency AS currency, "
            + "COALESCE(SUM(CASE WHEN t.entryType = :creditType THEN t.amount ELSE 0 END), 0) AS totalCredit, "
            + "COALESCE(SUM(CASE WHEN t.entryType = :debitType  THEN t.amount ELSE 0 END), 0) AS totalDebit, "
            + "COUNT(t) AS txnCount, "
            + "MAX(t.transactionDate) AS lastTxnDate "
            + "FROM Transaction t "
            + "WHERE t.userId = :userId AND t.deletedAt IS NULL AND t.transactionDate <= :asOf";

    String BALANCE_AGG_GROUP_BY = " GROUP BY t.accountId, t.currency";

    @Query(BALANCE_AGG_SELECT + BALANCE_AGG_GROUP_BY)
    List<AccountBalanceAggregate> aggregateBalances(
            @Param("userId") String userId,
            @Param("asOf") LocalDate asOf,
            @Param("creditType") EntryType creditType,
            @Param("debitType") EntryType debitType);

    @Query(BALANCE_AGG_SELECT + " AND t.accountId IN :accountIds" + BALANCE_AGG_GROUP_BY)
    List<AccountBalanceAggregate> aggregateBalancesForAccounts(
            @Param("userId") String userId,
            @Param("asOf") LocalDate asOf,
            @Param("accountIds") Collection<UUID> accountIds,
            @Param("creditType") EntryType creditType,
            @Param("debitType") EntryType debitType);
}
