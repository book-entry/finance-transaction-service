package com.personal.finance.transaction.repository;

import com.personal.finance.transaction.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Category persistence — every finder filters {@code deleted_at IS NULL}.
 * Spec §1.4: a partial unique index enforces name uniqueness per user, but
 * Hibernate {@code ddl-auto} cannot create partial indexes (see TODO on
 * {@link Category}); uniqueness is enforced in the service layer.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c FROM Category c WHERE c.categoryId = :id AND c.userId = :userId AND c.deletedAt IS NULL")
    Optional<Category> findActiveByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.name = :name AND c.deletedAt IS NULL")
    Optional<Category> findActiveByNameAndUserId(@Param("name") String name, @Param("userId") String userId);

    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findActiveByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Category c WHERE c.userId = :userId AND c.name IN :names AND c.deletedAt IS NULL")
    List<Category> findActiveByUserIdAndNameIn(@Param("userId") String userId, @Param("names") Collection<String> names);

    /**
     * Atomic-delete WRITE 1 — see {@code CategoryServiceImpl.deleteCategory}.
     * Returns the number of rows updated so the caller can detect a missed
     * delete (e.g. someone else just removed it).
     */
    @Modifying
    @Query("UPDATE Category c SET c.deletedAt = :now WHERE c.categoryId = :id AND c.userId = :userId AND c.deletedAt IS NULL")
    int softDelete(@Param("id") UUID id, @Param("userId") String userId, @Param("now") OffsetDateTime now);
}
