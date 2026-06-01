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
import com.personal.finance.transaction.mapper.CategoryMapperImpl;
import com.personal.finance.transaction.repository.CategoryRepository;
import com.personal.finance.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    private static final String USER_ID = "user-456";

    @Mock CategoryRepository categoryRepository;
    @Mock TransactionRepository transactionRepository;

    CategoryServiceImpl service;
    CategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CategoryMapperImpl();
        service = new CategoryServiceImpl(categoryRepository, transactionRepository, mapper);
    }

    // ── createCategory ───────────────────────────────────────────────────

    @Test
    void createCategory_givenValidRequest_thenReturns201Dto() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Food & Dining");
        req.setColourHex("#4ade80");
        when(categoryRepository.findActiveByNameAndUserId("Food & Dining", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setCategoryId(UUID.randomUUID());
            return c;
        });

        CategoryResponse resp = service.createCategory(USER_ID, req);

        assertThat(resp.getName()).isEqualTo("Food & Dining");
        assertThat(resp.getColourHex()).isEqualTo("#4ade80");
    }

    @Test
    void createCategory_givenDuplicateName_thenThrows409() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Dupes");
        when(categoryRepository.findActiveByNameAndUserId("Dupes", USER_ID))
                .thenReturn(Optional.of(categoryEntity("Dupes")));

        assertThatThrownBy(() -> service.createCategory(USER_ID, req))
                .isInstanceOf(CategoryNameConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    // ── listCategories ───────────────────────────────────────────────────

    @Test
    void listCategories_returnsActiveSortedByName() {
        when(categoryRepository.findActiveByUserId(USER_ID))
                .thenReturn(List.of(categoryEntity("Apple"), categoryEntity("Banana")));

        List<CategoryResponse> result = service.listCategories(USER_ID);

        assertThat(result).extracting(CategoryResponse::getName).containsExactly("Apple", "Banana");
    }

    // ── bulkUpsert ───────────────────────────────────────────────────────

    @Test
    void bulkUpsert_givenMixOfNewAndExisting_thenCountsAndReturnsAll() {
        BulkCategoryItem a = item("Apple");
        BulkCategoryItem b = item("Banana");
        BulkCategoryItem c = item("Cherry");
        when(categoryRepository.findActiveByUserIdAndNameIn(eq(USER_ID), any()))
                .thenReturn(List.of(categoryEntity("Apple")));  // Apple already exists
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category cat = inv.getArgument(0);
            cat.setCategoryId(UUID.randomUUID());
            return cat;
        });

        CategoryBulkResponse resp = service.bulkUpsert(USER_ID, List.of(a, b, c));

        assertThat(resp.getCreated()).isEqualTo(2);  // Banana + Cherry
        assertThat(resp.getSkipped()).isEqualTo(1);  // Apple
        assertThat(resp.getCategories()).hasSize(3);
    }

    @Test
    void bulkUpsert_givenDuplicateNamesInRequest_thenDeduplicatesAndCountsAsSkipped() {
        when(categoryRepository.findActiveByUserIdAndNameIn(eq(USER_ID), any())).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryBulkResponse resp = service.bulkUpsert(USER_ID,
                List.of(item("Same"), item("Same"), item("Same")));

        assertThat(resp.getCreated()).isEqualTo(1);
        assertThat(resp.getSkipped()).isEqualTo(2);
    }

    // ── summary ──────────────────────────────────────────────────────────

    @Test
    void summary_givenLinkedTransactions_thenReturnsCountAndSum() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(categoryEntity("Cat")));
        when(transactionRepository.countActiveByCategory(id, USER_ID)).thenReturn(3L);
        when(transactionRepository.sumActiveByCategory(id, USER_ID)).thenReturn(new BigDecimal("125.50"));
        when(transactionRepository.pickCurrencyForCategory(id, USER_ID)).thenReturn("USD");

        CategorySummaryResponse resp = service.summary(USER_ID, id);

        assertThat(resp.getTransactionCount()).isEqualTo(3L);
        assertThat(resp.getTotalAmount()).isEqualByComparingTo("125.50");
        assertThat(resp.getCurrency()).isEqualTo("USD");
    }

    @Test
    void summary_givenNoTransactions_thenZeroCountAndNullCurrency() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(categoryEntity("Cat")));
        when(transactionRepository.countActiveByCategory(id, USER_ID)).thenReturn(0L);
        when(transactionRepository.sumActiveByCategory(id, USER_ID)).thenReturn(BigDecimal.ZERO);

        CategorySummaryResponse resp = service.summary(USER_ID, id);

        assertThat(resp.getTransactionCount()).isZero();
        assertThat(resp.getCurrency()).isNull();
    }

    @Test
    void summary_givenNonExistentCategory_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summary(USER_ID, id))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // ── updateCategory ───────────────────────────────────────────────────

    @Test
    void updateCategory_givenRename_thenAppliesAndSaves() {
        UUID id = UUID.randomUUID();
        Category existing = categoryEntity("Old");
        existing.setCategoryId(id);
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findActiveByNameAndUserId("New", USER_ID)).thenReturn(Optional.empty());

        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("New");

        CategoryResponse resp = service.updateCategory(USER_ID, id, req);

        assertThat(resp.getName()).isEqualTo("New");
    }

    @Test
    void updateCategory_givenRenameToExistingName_thenThrows409() {
        UUID id = UUID.randomUUID();
        Category existing = categoryEntity("Old");
        existing.setCategoryId(id);
        Category other = categoryEntity("Taken");
        other.setCategoryId(UUID.randomUUID());
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findActiveByNameAndUserId("Taken", USER_ID)).thenReturn(Optional.of(other));

        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Taken");

        assertThatThrownBy(() -> service.updateCategory(USER_ID, id, req))
                .isInstanceOf(CategoryNameConflictException.class);
    }

    @Test
    void updateCategory_givenNonExistent_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(USER_ID, id, new UpdateCategoryRequest()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // ── deleteCategory — atomic double-write ─────────────────────────────

    @Test
    void deleteCategory_happyPath_thenSoftDeletesAndClearsLinkedTx() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(categoryEntity("X")));
        when(categoryRepository.softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class))).thenReturn(1);
        when(transactionRepository.clearCategory(id, USER_ID)).thenReturn(5);

        service.deleteCategory(USER_ID, id);

        verify(categoryRepository).softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class));
        verify(transactionRepository).clearCategory(id, USER_ID);
    }

    @Test
    void deleteCategory_whenRowDisappearsBetweenLoadAndSoftDelete_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID))
                .thenReturn(Optional.of(categoryEntity("X")));
        when(categoryRepository.softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.deleteCategory(USER_ID, id))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(transactionRepository, never()).clearCategory(any(), anyString());
    }

    @Test
    void deleteCategory_givenNonExistentId_thenThrows404_andSkipsWrites() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCategory(USER_ID, id))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).softDelete(any(), anyString(), any());
        verify(transactionRepository, never()).clearCategory(any(), anyString());
    }

    // ── resolveByName (inline create) ────────────────────────────────────

    @Test
    void resolveByName_givenExistingName_thenReturnsExistingAndFalse() {
        Category existing = categoryEntity("Snacks");
        when(categoryRepository.findActiveByNameAndUserId("Snacks", USER_ID))
                .thenReturn(Optional.of(existing));

        CategoryService.ResolvedCategory resolved = service.resolveByName(USER_ID, "Snacks");

        assertThat(resolved.category()).isSameAs(existing);
        assertThat(resolved.created()).isFalse();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void resolveByName_givenNewName_thenInsertsAndReturnsTrue() {
        when(categoryRepository.findActiveByNameAndUserId("Fresh", USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setCategoryId(UUID.randomUUID());
            return c;
        });

        CategoryService.ResolvedCategory resolved = service.resolveByName(USER_ID, "Fresh");

        assertThat(resolved.category().getName()).isEqualTo("Fresh");
        assertThat(resolved.created()).isTrue();
    }

    // ── loadOwnedById ────────────────────────────────────────────────────

    @Test
    void loadOwnedById_givenNotOwned_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadOwnedById(USER_ID, id))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Category categoryEntity(String name) {
        return Category.builder()
                .categoryId(UUID.randomUUID())
                .userId(USER_ID)
                .name(name)
                .build();
    }

    private BulkCategoryItem item(String name) {
        BulkCategoryItem i = new BulkCategoryItem();
        i.setName(name);
        return i;
    }
}
