package com.personal.finance.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Categories table — spec §1.4. Flat user-owned labels for transactions.
 * Soft-deleted only.
 *
 * <p><b>TODO (production):</b> the spec calls for a partial unique index
 * {@code UNIQUE (user_id, name) WHERE deleted_at IS NULL} which allows name
 * reuse after deletion. Hibernate's {@code ddl-auto=update} cannot create
 * partial indexes; add it manually:
 *
 * <pre>
 *   CREATE UNIQUE INDEX uk_categories_user_name_active
 *   ON categories (user_id, name)
 *   WHERE deleted_at IS NULL;
 * </pre>
 *
 * For development the uniqueness is enforced in
 * {@code CategoryServiceImpl} (SELECT-then-INSERT inside a transaction).
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @UuidGenerator
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** e.g. "#4ade80". Optional UI hint. */
    @Column(name = "colour_hex", length = 7)
    private String colourHex;

    /** NULL = active. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
