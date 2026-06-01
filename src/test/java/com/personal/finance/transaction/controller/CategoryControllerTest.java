package com.personal.finance.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.finance.common.web.ApiResponseBodyAdvice;
import com.personal.finance.common.web.GlobalExceptionHandler;
import com.personal.finance.transaction.dto.request.CreateCategoryRequest;
import com.personal.finance.transaction.dto.request.UpdateCategoryRequest;
import com.personal.finance.transaction.dto.response.CategoryBulkResponse;
import com.personal.finance.transaction.dto.response.CategoryResponse;
import com.personal.finance.transaction.dto.response.CategorySummaryResponse;
import com.personal.finance.transaction.exception.CategoryNameConflictException;
import com.personal.finance.transaction.exception.CategoryNotFoundException;
import com.personal.finance.transaction.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class CategoryControllerTest {

    private static final String USER_ID = "user-789";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CategoryService categoryService;

    @Test
    void createCategory_givenValidRequest_thenReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.createCategory(eq(USER_ID), any())).thenReturn(categoryResponse(id, "Food"));
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Food");
        req.setColourHex("#4ade80");

        mvc.perform(post("/v1/categories")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryId").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Food"));
    }

    @Test
    void createCategory_givenMissingName_thenReturns400() throws Exception {
        mvc.perform(post("/v1/categories")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void createCategory_givenMissingUserId_thenReturns400() throws Exception {
        mvc.perform(post("/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void createCategory_givenDuplicateName_thenReturns409() throws Exception {
        when(categoryService.createCategory(eq(USER_ID), any()))
                .thenThrow(new CategoryNameConflictException("Dupes"));
        mvc.perform(post("/v1/categories")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dupes\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NAME_CONFLICT"));
    }

    @Test
    void listCategories_returns200_andWrappedArray() throws Exception {
        when(categoryService.listCategories(USER_ID))
                .thenReturn(List.of(categoryResponse(UUID.randomUUID(), "A")));
        mvc.perform(get("/v1/categories").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void bulkUpsert_givenValidArray_thenReturns201() throws Exception {
        when(categoryService.bulkUpsert(eq(USER_ID), any())).thenReturn(CategoryBulkResponse.builder()
                .created(1).skipped(1).categories(List.of()).build());
        mvc.perform(post("/v1/categories/bulk")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"X\"},{\"name\":\"Y\"}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.created").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1));
    }

    @Test
    void bulkUpsert_givenEmptyArray_thenReturns400() throws Exception {
        mvc.perform(post("/v1/categories/bulk")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summary_givenExistingCategory_thenReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.summary(USER_ID, id)).thenReturn(CategorySummaryResponse.builder()
                .categoryId(id).categoryName("Food").transactionCount(3L)
                .totalAmount(new BigDecimal("100.00")).currency("USD").build());

        mvc.perform(get("/v1/categories/{id}/summary", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionCount").value(3));
    }

    @Test
    void summary_givenNonExistentCategory_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.summary(USER_ID, id)).thenThrow(new CategoryNotFoundException("Cat " + id));

        mvc.perform(get("/v1/categories/{id}/summary", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void updateCategory_givenValidRename_thenReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.updateCategory(eq(USER_ID), eq(id), any()))
                .thenReturn(categoryResponse(id, "Renamed"));
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Renamed");

        mvc.perform(put("/v1/categories/{id}", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed"));
    }

    @Test
    void updateCategory_givenNameConflict_thenReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.updateCategory(eq(USER_ID), eq(id), any()))
                .thenThrow(new CategoryNameConflictException("Taken"));

        mvc.perform(put("/v1/categories/{id}", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Taken\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NAME_CONFLICT"));
    }

    @Test
    void deleteCategory_givenExistingId_thenReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/v1/categories/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_givenNonExistentId_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new CategoryNotFoundException("missing")).when(categoryService).deleteCategory(USER_ID, id);

        mvc.perform(delete("/v1/categories/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    private CategoryResponse categoryResponse(UUID id, String name) {
        return CategoryResponse.builder()
                .categoryId(id)
                .userId(USER_ID)
                .name(name)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
