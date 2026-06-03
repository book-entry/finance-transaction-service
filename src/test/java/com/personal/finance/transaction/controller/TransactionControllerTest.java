package com.personal.finance.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.finance.common.web.ApiResponseBodyAdvice;
import com.personal.finance.common.web.GlobalExceptionHandler;
<<<<<<< Updated upstream
import com.personal.finance.transaction.dto.request.BulkCategoryRequest;
import com.personal.finance.transaction.dto.request.BulkDeleteRequest;
=======
import com.personal.finance.common.exception.ValidationException;
>>>>>>> Stashed changes
import com.personal.finance.transaction.dto.request.CategorisePatchRequest;
import com.personal.finance.transaction.dto.request.CreateTransactionRequest;
import com.personal.finance.transaction.dto.response.BalancesResponse;
import com.personal.finance.transaction.dto.response.BatchInsertResponse;
import com.personal.finance.transaction.dto.response.BulkCategoryResponse;
import com.personal.finance.transaction.dto.response.BulkDeleteResponse;
import com.personal.finance.transaction.dto.response.CategorisedTransactionResponse;
import com.personal.finance.transaction.dto.response.CategoryRefResponse;
import com.personal.finance.transaction.dto.response.CountsResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import com.personal.finance.transaction.exception.AccountClosedException;
import com.personal.finance.transaction.exception.AccountNotFoundException;
import com.personal.finance.transaction.exception.InvalidCategoryRequestException;
import com.personal.finance.transaction.exception.TransactionNotFoundException;
import com.personal.finance.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class TransactionControllerTest {

    private static final String USER_ID = "user-789";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean TransactionService transactionService;

    @Test
    void createTransaction_givenValidRequest_thenReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.createTransaction(eq(USER_ID), any())).thenReturn(sampleResponse(id, null));

        mvc.perform(post("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value(id.toString()))
                .andExpect(jsonPath("$.data.source").value("MANUAL"));
    }

    @Test
    void createTransaction_givenMissingAccountId_thenReturns400() throws Exception {
        CreateTransactionRequest req = createRequest();
        req.setAccountId(null);

        mvc.perform(post("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void createTransaction_givenMissingUserId_thenReturns400() throws Exception {
        mvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_whenAccountNotFound_thenReturns404() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.createTransaction(eq(USER_ID), any()))
                .thenThrow(new AccountNotFoundException(accountId));

        mvc.perform(post("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void createTransaction_whenAccountClosed_thenReturns422() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.createTransaction(eq(USER_ID), any()))
                .thenThrow(new AccountClosedException(accountId));

        mvc.perform(post("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_CLOSED"));
    }

    @Test
    void listTransactions_returns200WithPageMetadata() throws Exception {
        when(transactionService.listTransactions(eq(USER_ID), any(), any(), any(), anyBoolean(),
                                                 any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(TransactionPageResponse.builder()
                        .data(List.of(sampleResponse(UUID.randomUUID(), null)))
                        .total(1L).page(1).size(50).build());

        mvc.perform(get("/v1/transactions").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.data[0].source").value("MANUAL"));
    }

    @Test
    void listTransactions_givenSearchAndCategoryIds_passesParamsThrough() throws Exception {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(transactionService.listTransactions(eq(USER_ID), any(), any(),
                eq(List.of(c1, c2)), eq(false), any(), any(), eq("ParknShop"), anyInt(), anyInt()))
                .thenReturn(TransactionPageResponse.builder()
                        .data(List.of()).total(0L).page(1).size(50).build());

        mvc.perform(get("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("categoryIds", c1.toString() + "," + c2.toString())
                        .param("q", "ParknShop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void listTransactions_whenServiceRejectsFilterMix_returns400() throws Exception {
        when(transactionService.listTransactions(eq(USER_ID), any(), any(), any(), anyBoolean(),
                                                 any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new ValidationException("categoryFilter", "mutually exclusive"));

        mvc.perform(get("/v1/transactions")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("categoryId", UUID.randomUUID().toString())
                        .param("uncategorized", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VAL_001"));
    }

    @Test
    void counts_returns200WithTotalUncategorizedAndByCategory() throws Exception {
        UUID catA = UUID.randomUUID();
        when(transactionService.listCounts(USER_ID))
                .thenReturn(CountsResponse.builder()
                        .total(1843L)
                        .uncategorized(47L)
                        .byCategory(java.util.Map.of(catA, 312L))
                        .build());

        mvc.perform(get("/v1/transactions/counts").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1843))
                .andExpect(jsonPath("$.data.uncategorized").value(47))
                .andExpect(jsonPath("$.data.byCategory." + catA.toString()).value(312));
    }

    @Test
    void counts_givenMissingUserId_returns400() throws Exception {
        mvc.perform(get("/v1/transactions/counts"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_givenNonExistent_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.getTransaction(USER_ID, id))
                .thenThrow(new TransactionNotFoundException("missing"));

        mvc.perform(get("/v1/transactions/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void categorise_givenBothFields_thenReturns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactionService.categorise(eq(USER_ID), eq(id), any()))
                .thenThrow(new InvalidCategoryRequestException());

        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryId(UUID.randomUUID());
        req.setCategoryName("X");

        mvc.perform(patch("/v1/transactions/{id}/category", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY_REQUEST"));
    }

    @Test
    void categorise_withNewCategoryName_thenReturns200WithIsNewTrue() throws Exception {
        UUID id = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CategoryRefResponse cat = CategoryRefResponse.builder().id(catId).name("New").isNew(true).build();
        when(transactionService.categorise(eq(USER_ID), eq(id), any()))
                .thenReturn(CategorisedTransactionResponse.builder()
                        .transaction(sampleResponse(id, cat))
                        .category(cat)
                        .build());

        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryName("New");

        mvc.perform(patch("/v1/transactions/{id}/category", id)
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category.isNew").value(true))
                .andExpect(jsonPath("$.data.category.name").value("New"));
    }

    @Test
    void deleteTransaction_givenExisting_thenReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/v1/transactions/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_givenNonExistent_thenReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TransactionNotFoundException("missing")).when(transactionService).deleteTransaction(USER_ID, id);

        mvc.perform(delete("/v1/transactions/{id}", id).header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void balances_givenNoQueryParams_returns200WithAggregates() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.listBalances(eq(USER_ID), any(), any()))
                .thenReturn(BalancesResponse.builder()
                        .asOf(LocalDate.of(2026, 6, 2))
                        .balances(List.of(BalancesResponse.AccountBalance.builder()
                                .accountId(accountId)
                                .currency("HKD")
                                .totalCredit(new BigDecimal("56800.00"))
                                .totalDebit(new BigDecimal("18420.50"))
                                .balance(new BigDecimal("38379.50"))
                                .txnCount(42L)
                                .lastTxnDate(LocalDate.of(2026, 5, 31))
                                .build()))
                        .build());

        mvc.perform(get("/v1/transactions/balances").header(USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asOf").value("2026-06-02"))
                .andExpect(jsonPath("$.data.balances[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.data.balances[0].currency").value("HKD"))
                .andExpect(jsonPath("$.data.balances[0].balance").value(38379.50))
                .andExpect(jsonPath("$.data.balances[0].txnCount").value(42))
                .andExpect(jsonPath("$.data.balances[0].lastTxnDate").value("2026-05-31"));
    }

    @Test
    void balances_givenAsOfAndAccountIds_passesParamsToService() throws Exception {
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        LocalDate asOf = LocalDate.of(2026, 5, 1);
        when(transactionService.listBalances(USER_ID, asOf, List.of(a1, a2)))
                .thenReturn(BalancesResponse.builder().asOf(asOf).balances(List.of()).build());

        mvc.perform(get("/v1/transactions/balances")
                        .header(USER_ID_HEADER, USER_ID)
                        .param("asOf", "2026-05-01")
                        .param("accountIds", a1.toString() + "," + a2.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asOf").value("2026-05-01"))
                .andExpect(jsonPath("$.data.balances").isArray());
    }

    @Test
    void balances_givenMissingUserId_thenReturns400() throws Exception {
        mvc.perform(get("/v1/transactions/balances"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkCategory_givenValidPayload_returns200WithSplitCounts() throws Exception {
        UUID catId = UUID.randomUUID();
        UUID notFoundId = UUID.randomUUID();
        when(transactionService.bulkSetCategory(eq(USER_ID), any()))
                .thenReturn(BulkCategoryResponse.builder()
                        .updated(47)
                        .skipped(0)
                        .notFound(List.of(notFoundId))
                        .category(CategoryRefResponse.builder().id(catId).name("Coffee").isNew(false).build())
                        .build());

        BulkCategoryRequest req = new BulkCategoryRequest();
        req.setTransactionIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        req.setCategoryId(catId);

        mvc.perform(patch("/v1/transactions/bulk-category")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(47))
                .andExpect(jsonPath("$.data.skipped").value(0))
                .andExpect(jsonPath("$.data.notFound[0]").value(notFoundId.toString()))
                .andExpect(jsonPath("$.data.category.id").value(catId.toString()))
                .andExpect(jsonPath("$.data.category.isNew").value(false));
    }

    @Test
    void bulkCategory_givenEmptyTransactionIds_returns400() throws Exception {
        BulkCategoryRequest req = new BulkCategoryRequest();
        req.setTransactionIds(List.of());
        req.setCategoryId(UUID.randomUUID());

        mvc.perform(patch("/v1/transactions/bulk-category")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkCategory_givenBothCategoryFields_returns400() throws Exception {
        when(transactionService.bulkSetCategory(eq(USER_ID), any()))
                .thenThrow(new com.personal.finance.transaction.exception.InvalidCategoryRequestException());

        BulkCategoryRequest req = new BulkCategoryRequest();
        req.setTransactionIds(List.of(UUID.randomUUID()));
        req.setCategoryId(UUID.randomUUID());
        req.setCategoryName("X");

        mvc.perform(patch("/v1/transactions/bulk-category")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY_REQUEST"));
    }

    @Test
    void bulkDelete_givenValidPayload_returns200WithSplitCounts() throws Exception {
        UUID notFoundId = UUID.randomUUID();
        when(transactionService.bulkDelete(eq(USER_ID), any()))
                .thenReturn(BulkDeleteResponse.builder()
                        .deleted(3)
                        .notFound(List.of(notFoundId))
                        .build());

        BulkDeleteRequest req = new BulkDeleteRequest();
        req.setTransactionIds(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), notFoundId));

        mvc.perform(delete("/v1/transactions/bulk")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(3))
                .andExpect(jsonPath("$.data.notFound[0]").value(notFoundId.toString()));
    }

    @Test
    void bulkDelete_givenEmptyTransactionIds_returns400() throws Exception {
        BulkDeleteRequest req = new BulkDeleteRequest();
        req.setTransactionIds(List.of());

        mvc.perform(delete("/v1/transactions/bulk")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batch_givenValidPayload_thenReturns200WithCounts() throws Exception {
        when(transactionService.insertBatch(eq(USER_ID), any()))
                .thenReturn(BatchInsertResponse.builder()
                        .insertedCount(2).failedRows(List.of()).build());

        String body = "{\"bulkJobId\":\"" + UUID.randomUUID() + "\",\"rows\":[{"
                + "\"accountId\":\"" + UUID.randomUUID() + "\","
                + "\"entryType\":\"DEBIT\",\"amount\":1.00,\"currency\":\"USD\","
                + "\"transactionDate\":\"2026-05-23\"}]}";

        mvc.perform(post("/v1/transactions/batch")
                        .header(USER_ID_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.insertedCount").value(2));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CreateTransactionRequest createRequest() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setAccountId(UUID.randomUUID());
        req.setEntryType(EntryType.DEBIT);
        req.setAmount(new BigDecimal("123.45"));
        req.setCurrency("USD");
        req.setTransactionDate(LocalDate.of(2026, 5, 23));
        return req;
    }

    private TransactionResponse sampleResponse(UUID id, CategoryRefResponse cat) {
        return TransactionResponse.builder()
                .transactionId(id)
                .accountId(UUID.randomUUID())
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .transactionDate(LocalDate.of(2026, 5, 23))
                .source(Source.MANUAL)
                .category(cat)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
