package com.personal.finance.transaction.controller;

import com.personal.finance.transaction.dto.request.BulkCategoryItem;
import com.personal.finance.transaction.dto.request.CreateCategoryRequest;
import com.personal.finance.transaction.dto.request.UpdateCategoryRequest;
import com.personal.finance.transaction.dto.response.CategoryBulkResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.dto.response.CategorySummaryResponse;
import com.personal.finance.transaction.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST entry points for categories — spec §3.3. Routing only. */
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final CategoryService categoryService;

    /** Spec §3.3 — {@code POST /v1/categories}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestHeader(USER_ID_HEADER) String userId,
                                   @Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(userId, request);
    }

    /** Spec §3.3 — {@code GET /v1/categories}. */
    @GetMapping
    public List<CategoryResponse> list(@RequestHeader(USER_ID_HEADER) String userId) {
        return categoryService.listCategories(userId);
    }

    /** Spec §3.3 — {@code POST /v1/categories/bulk}. */
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBulkResponse bulk(@RequestHeader(USER_ID_HEADER) String userId,
                                     @Valid @NotEmpty @RequestBody List<@Valid BulkCategoryItem> items) {
        return categoryService.bulkUpsert(userId, items);
    }

    /** Spec §3.3 — {@code GET /v1/categories/{id}/summary}. */
    @GetMapping("/{id}/summary")
    public CategorySummaryResponse summary(@RequestHeader(USER_ID_HEADER) String userId,
                                           @PathVariable("id") UUID id) {
        return categoryService.summary(userId, id);
    }

    /** Spec §3.3 — {@code PUT /v1/categories/{id}}. */
    @PutMapping("/{id}")
    public CategoryResponse update(@RequestHeader(USER_ID_HEADER) String userId,
                                   @PathVariable("id") UUID id,
                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.updateCategory(userId, id, request);
    }

    /** Spec §3.3 — {@code DELETE /v1/categories/{id}}. Returns 204. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(USER_ID_HEADER) String userId,
                       @PathVariable("id") UUID id) {
        categoryService.deleteCategory(userId, id);
    }
}
