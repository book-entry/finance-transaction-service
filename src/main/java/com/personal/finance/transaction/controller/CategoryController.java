package com.personal.finance.transaction.controller;

import com.personal.finance.transaction.dto.request.BulkCategoryItem;
import com.personal.finance.transaction.dto.request.CreateCategoryRequest;
import com.personal.finance.transaction.dto.request.UpdateCategoryRequest;
import com.personal.finance.transaction.dto.response.CategoryBulkResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.dto.response.CategorySummaryResponse;
import com.personal.finance.transaction.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Categories", description = "Manage user-defined spending categories used to classify transactions.")
public class CategoryController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final CategoryService categoryService;

    /** Spec §3.3 — {@code POST /v1/categories}. */
    @Operation(
            summary = "Create a category",
            description = "Creates a new named category for the authenticated user. "
                    + "The name must be unique (case-sensitive) among the user's active categories. "
                    + "Returns 409 when a category with the same name already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — name blank or colourHex invalid",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists for this user",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(userId, request);
    }

    /** Spec §3.3 — {@code GET /v1/categories}. */
    @Operation(
            summary = "List categories",
            description = "Returns all active categories owned by the authenticated user, sorted by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of categories",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping
    public List<CategoryResponse> list(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId) {
        return categoryService.listCategories(userId);
    }

    /** Spec §3.3 — {@code POST /v1/categories/bulk}. */
    @Operation(
            summary = "Bulk upsert categories",
            description = "Idempotently creates up to 500 categories in a single request. "
                    + "Categories whose name already exists for the user are skipped (not updated). "
                    + "Duplicate names within the request body are also de-duplicated and counted as skipped. "
                    + "Returns counts of created and skipped items alongside the resolved (categoryId, name) pairs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bulk upsert result",
                    content = @Content(schema = @Schema(implementation = CategoryBulkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Empty list or per-item validation failure",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBulkResponse bulk(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @NotEmpty @RequestBody List<@Valid BulkCategoryItem> items) {
        return categoryService.bulkUpsert(userId, items);
    }

    /** Spec §3.3 — {@code GET /v1/categories/{id}/summary}. */
    @Operation(
            summary = "Get category summary",
            description = "Returns aggregate statistics for a single category: transaction count, total amount, "
                    + "and the currency sampled from the first transaction. Intended for the delete-confirmation dialog. "
                    + "Returns 404 if the category does not exist or is not owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category summary",
                    content = @Content(schema = @Schema(implementation = CategorySummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping("/{id}/summary")
    public CategorySummaryResponse summary(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Category UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
            @PathVariable("id") UUID id) {
        return categoryService.summary(userId, id);
    }

    /** Spec §3.3 — {@code PUT /v1/categories/{id}}. */
    @Operation(
            summary = "Update a category",
            description = "Renames or recolours an existing category. Only supplied (non-null) fields are applied. "
                    + "Returns 409 if the new name conflicts with another active category owned by the user. "
                    + "Returns 404 if the category does not exist or is not owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — name too long or colourHex invalid",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists for this user",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public CategoryResponse update(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Category UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.updateCategory(userId, id, request);
    }

    /** Spec §3.3 — {@code DELETE /v1/categories/{id}}. Returns 204. */
    @Operation(
            summary = "Delete a category",
            description = "Soft-deletes a category and atomically clears the categoryId on all linked active transactions "
                    + "(both writes occur in one transaction). "
                    + "Returns 404 if the category does not exist or is not owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted — no body"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(USER_ID_HEADER) String userId,
            @Parameter(description = "Category UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
            @PathVariable("id") UUID id) {
        categoryService.deleteCategory(userId, id);
    }
}
