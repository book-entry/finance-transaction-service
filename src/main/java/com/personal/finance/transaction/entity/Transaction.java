package com.personal.finance.transaction.entity;

import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactions table — spec §1.3. Immutable after creation; soft-deleted only.
 * {@code account_id} and {@code category_id} are logical FKs (no DB
 * constraint) since they may reference rows in another service's database.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_user_id",           columnList = "user_id"),
        @Index(name = "idx_tx_account_id",        columnList = "account_id"),
        @Index(name = "idx_tx_category_id",       columnList = "category_id"),
        @Index(name = "idx_tx_user_date",         columnList = "user_id, transaction_date"),
        @Index(name = "idx_tx_deleted_at",        columnList = "deleted_at"),
        @Index(name = "idx_tx_bulk_job_id",       columnList = "bulk_job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @UuidGenerator
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** Logical FK to {@code account_db.accounts}. */
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /** Nullable — {@code null} means uncategorised, or category was deleted. */
    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private Source source;

    /** Set when {@code source=BULK}. Lets you query all rows from a job. */
    @Column(name = "bulk_job_id")
    private UUID bulkJobId;

    /** NULL = active. Soft delete — never physically removed. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
