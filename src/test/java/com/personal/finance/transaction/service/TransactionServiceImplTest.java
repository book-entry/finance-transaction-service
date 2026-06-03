package com.personal.finance.transaction.service;

import com.personal.finance.transaction.client.account.AccountServiceClient;
import com.personal.finance.transaction.client.account.AccountSummary;
import com.personal.finance.transaction.dto.request.BatchTransactionsRequest;
import com.personal.finance.transaction.dto.request.CategorisePatchRequest;
import com.personal.finance.transaction.dto.request.CreateTransactionRequest;
import com.personal.finance.transaction.dto.response.BalancesResponse;
import com.personal.finance.transaction.dto.response.BatchInsertResponse;
import com.personal.finance.transaction.dto.response.CategorisedTransactionResponse;
import com.personal.finance.transaction.dto.response.TransactionPageResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;
import com.personal.finance.transaction.entity.Category;
import com.personal.finance.transaction.entity.Transaction;
import com.personal.finance.transaction.enums.AccountStatus;
import com.personal.finance.transaction.enums.EntryType;
import com.personal.finance.transaction.enums.Source;
import com.personal.finance.transaction.exception.AccountClosedException;
import com.personal.finance.transaction.exception.AccountNotFoundException;
import com.personal.finance.transaction.exception.CategoryNotFoundException;
import com.personal.finance.transaction.exception.InvalidCategoryRequestException;
import com.personal.finance.transaction.exception.TransactionNotFoundException;
import com.personal.finance.transaction.mapper.TransactionMapperImpl;
import com.personal.finance.transaction.repository.TransactionRepository;
import com.personal.finance.transaction.repository.projection.AccountBalanceAggregate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class TransactionServiceImplTest {

    private static final String USER_ID = "user-789";

    @Mock TransactionRepository transactionRepository;
    @Mock CategoryService categoryService;
    @Mock AccountServiceClient accountClient;

    TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(
                transactionRepository, categoryService, accountClient, new TransactionMapperImpl());
    }

    // ── createTransaction ────────────────────────────────────────────────

    @Test
    void createTransaction_givenValidRequest_thenReturns201_andCallsAccountService() {
        CreateTransactionRequest req = txRequest();
        when(accountClient.fetchActiveAccount(USER_ID, req.getAccountId()))
                .thenReturn(activeAccountSummary(req.getAccountId()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setTransactionId(UUID.randomUUID());
            t.setCreatedAt(OffsetDateTime.now());
            return t;
        });

        TransactionResponse resp = service.createTransaction(USER_ID, req);

        assertThat(resp.getTransactionId()).isNotNull();
        assertThat(resp.getSource()).isEqualTo(Source.MANUAL);
        verify(accountClient).fetchActiveAccount(USER_ID, req.getAccountId());
    }

    @Test
    void createTransaction_givenCategoryName_thenInlineCreatesViaCategoryService() {
        CreateTransactionRequest req = txRequest();
        req.setCategoryName("Inline Cat");
        when(accountClient.fetchActiveAccount(USER_ID, req.getAccountId()))
                .thenReturn(activeAccountSummary(req.getAccountId()));
        Category cat = Category.builder().categoryId(UUID.randomUUID()).name("Inline Cat").userId(USER_ID).build();
        when(categoryService.resolveByName(USER_ID, "Inline Cat"))
                .thenReturn(new CategoryService.ResolvedCategory(cat, true));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse resp = service.createTransaction(USER_ID, req);

        assertThat(resp.getCategory()).isNotNull();
        assertThat(resp.getCategory().isNew()).isTrue();
    }

    @Test
    void createTransaction_givenCategoryId_thenLoadsExistingCategory() {
        CreateTransactionRequest req = txRequest();
        UUID catId = UUID.randomUUID();
        req.setCategoryId(catId);
        when(accountClient.fetchActiveAccount(USER_ID, req.getAccountId()))
                .thenReturn(activeAccountSummary(req.getAccountId()));
        Category cat = Category.builder().categoryId(catId).name("Existing").userId(USER_ID).build();
        when(categoryService.loadOwnedById(USER_ID, catId)).thenReturn(cat);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse resp = service.createTransaction(USER_ID, req);

        assertThat(resp.getCategory().getId()).isEqualTo(catId);
        assertThat(resp.getCategory().isNew()).isFalse();
    }

    @Test
    void createTransaction_whenAccountNotFound_thenThrows404_andSkipsSave() {
        CreateTransactionRequest req = txRequest();
        when(accountClient.fetchActiveAccount(USER_ID, req.getAccountId()))
                .thenThrow(new AccountNotFoundException(req.getAccountId()));

        assertThatThrownBy(() -> service.createTransaction(USER_ID, req))
                .isInstanceOf(AccountNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_whenAccountClosed_thenThrows422_andSkipsSave() {
        CreateTransactionRequest req = txRequest();
        when(accountClient.fetchActiveAccount(USER_ID, req.getAccountId()))
                .thenThrow(new AccountClosedException(req.getAccountId()));

        assertThatThrownBy(() -> service.createTransaction(USER_ID, req))
                .isInstanceOf(AccountClosedException.class);
        verify(transactionRepository, never()).save(any());
    }

    // ── listTransactions ─────────────────────────────────────────────────

    @Test
    void listTransactions_returnsPagedDtos() {
        Transaction tx = txEntity();
        Page<Transaction> page = new PageImpl<>(List.of(tx), Pageable.unpaged(), 1);
        when(transactionRepository.findActiveWithFilters(eq(USER_ID), any(), any(), any(), any(), any()))
                .thenReturn(page);

        TransactionPageResponse resp = service.listTransactions(USER_ID, null, null, null, null, 1, 50);

        assertThat(resp.getData()).hasSize(1);
        assertThat(resp.getTotal()).isEqualTo(1L);
        assertThat(resp.getPage()).isEqualTo(1);
        assertThat(resp.getSize()).isEqualTo(50);
    }

    // ── getTransaction ───────────────────────────────────────────────────

    @Test
    void getTransaction_givenExistingId_thenReturnsDto() {
        UUID id = UUID.randomUUID();
        Transaction tx = txEntity();
        tx.setTransactionId(id);
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(tx));

        TransactionResponse resp = service.getTransaction(USER_ID, id);

        assertThat(resp.getTransactionId()).isEqualTo(id);
    }

    @Test
    void getTransaction_givenNonExistent_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction(USER_ID, id))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    // ── categorise (PATCH) ───────────────────────────────────────────────

    @Test
    void categorise_givenBothFieldsProvided_thenThrows400() {
        UUID id = UUID.randomUUID();
        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryId(UUID.randomUUID());
        req.setCategoryName("Also Set");

        assertThatThrownBy(() -> service.categorise(USER_ID, id, req))
                .isInstanceOf(InvalidCategoryRequestException.class);
    }

    @Test
    void categorise_givenNeitherFieldProvided_thenThrows400() {
        UUID id = UUID.randomUUID();
        CategorisePatchRequest req = new CategorisePatchRequest();

        assertThatThrownBy(() -> service.categorise(USER_ID, id, req))
                .isInstanceOf(InvalidCategoryRequestException.class);
    }

    @Test
    void categorise_givenCategoryName_thenResolvesAndSetsIsNew() {
        UUID id = UUID.randomUUID();
        Transaction tx = txEntity();
        tx.setTransactionId(id);
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Category cat = Category.builder().categoryId(UUID.randomUUID()).name("Inline").userId(USER_ID).build();
        when(categoryService.resolveByName(USER_ID, "Inline"))
                .thenReturn(new CategoryService.ResolvedCategory(cat, true));

        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryName("Inline");

        CategorisedTransactionResponse resp = service.categorise(USER_ID, id, req);

        assertThat(resp.getCategory().isNew()).isTrue();
        assertThat(resp.getCategory().getName()).isEqualTo("Inline");
    }

    @Test
    void categorise_givenCategoryId_thenLoadsExisting_isNewFalse() {
        UUID id = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Transaction tx = txEntity();
        tx.setTransactionId(id);
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Category cat = Category.builder().categoryId(catId).name("Existing").userId(USER_ID).build();
        when(categoryService.loadOwnedById(USER_ID, catId)).thenReturn(cat);

        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryId(catId);

        CategorisedTransactionResponse resp = service.categorise(USER_ID, id, req);

        assertThat(resp.getCategory().isNew()).isFalse();
        assertThat(resp.getCategory().getId()).isEqualTo(catId);
    }

    @Test
    void categorise_givenNonExistentTransaction_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());
        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryName("X");

        assertThatThrownBy(() -> service.categorise(USER_ID, id, req))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void categorise_givenCategoryIdNotFound_thenThrows404() {
        UUID id = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        Transaction tx = txEntity();
        tx.setTransactionId(id);
        when(transactionRepository.findActiveByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(tx));
        when(categoryService.loadOwnedById(USER_ID, catId))
                .thenThrow(new CategoryNotFoundException("missing"));

        CategorisePatchRequest req = new CategorisePatchRequest();
        req.setCategoryId(catId);

        assertThatThrownBy(() -> service.categorise(USER_ID, id, req))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // ── deleteTransaction ────────────────────────────────────────────────

    @Test
    void deleteTransaction_happyPath_thenCallsSoftDelete() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class))).thenReturn(1);

        service.deleteTransaction(USER_ID, id);

        verify(transactionRepository).softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class));
    }

    @Test
    void deleteTransaction_givenNonExistent_thenThrows404() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.softDelete(eq(id), eq(USER_ID), any(OffsetDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.deleteTransaction(USER_ID, id))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    // ── listBalances ─────────────────────────────────────────────────────

    @Test
    void listBalances_givenNullAsOf_defaultsToTodayAndUsesUnfilteredQuery() {
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.aggregateBalances(
                eq(USER_ID), any(LocalDate.class), eq(EntryType.CREDIT), eq(EntryType.DEBIT)))
                .thenReturn(List.of(aggregate(accountId, "HKD",
                        new BigDecimal("56800.00"), new BigDecimal("18420.50"),
                        42L, LocalDate.of(2026, 5, 31))));

        BalancesResponse resp = service.listBalances(USER_ID, null, null);

        assertThat(resp.getAsOf()).isEqualTo(LocalDate.now());
        assertThat(resp.getBalances()).hasSize(1);
        BalancesResponse.AccountBalance b = resp.getBalances().get(0);
        assertThat(b.getAccountId()).isEqualTo(accountId);
        assertThat(b.getCurrency()).isEqualTo("HKD");
        assertThat(b.getTotalCredit()).isEqualByComparingTo("56800.00");
        assertThat(b.getTotalDebit()).isEqualByComparingTo("18420.50");
        assertThat(b.getBalance()).isEqualByComparingTo("38379.50");
        assertThat(b.getTxnCount()).isEqualTo(42L);
        assertThat(b.getLastTxnDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        verify(transactionRepository, never()).aggregateBalancesForAccounts(
                anyString(), any(), any(), any(), any());
    }

    @Test
    void listBalances_givenAccountIds_callsFilteredQueryAndPropagatesAsOf() {
        LocalDate asOf = LocalDate.of(2026, 6, 1);
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        List<UUID> ids = List.of(a1, a2);
        when(transactionRepository.aggregateBalancesForAccounts(
                eq(USER_ID), eq(asOf), eq(ids), eq(EntryType.CREDIT), eq(EntryType.DEBIT)))
                .thenReturn(List.of(
                        aggregate(a1, "HKD", new BigDecimal("100"), new BigDecimal("40"),
                                3L, LocalDate.of(2026, 5, 20)),
                        aggregate(a2, "USD", new BigDecimal("50"), new BigDecimal("0"),
                                1L, LocalDate.of(2026, 4, 10))));

        BalancesResponse resp = service.listBalances(USER_ID, asOf, ids);

        assertThat(resp.getAsOf()).isEqualTo(asOf);
        assertThat(resp.getBalances()).hasSize(2);
        assertThat(resp.getBalances().get(0).getBalance()).isEqualByComparingTo("60");
        assertThat(resp.getBalances().get(1).getBalance()).isEqualByComparingTo("50");
        verify(transactionRepository, never()).aggregateBalances(
                anyString(), any(), any(), any());
    }

    @Test
    void listBalances_givenEmptyAccountIds_shortCircuitsWithEmptyResponse() {
        LocalDate asOf = LocalDate.of(2026, 6, 1);

        BalancesResponse resp = service.listBalances(USER_ID, asOf, List.of());

        assertThat(resp.getAsOf()).isEqualTo(asOf);
        assertThat(resp.getBalances()).isEmpty();
        verify(transactionRepository, never()).aggregateBalances(
                anyString(), any(), any(), any());
        verify(transactionRepository, never()).aggregateBalancesForAccounts(
                anyString(), any(), any(), any(), any());
    }

    @Test
    void listBalances_whenNoActiveTransactions_returnsEmptyBalances() {
        when(transactionRepository.aggregateBalances(
                eq(USER_ID), any(LocalDate.class), eq(EntryType.CREDIT), eq(EntryType.DEBIT)))
                .thenReturn(List.of());

        BalancesResponse resp = service.listBalances(USER_ID, null, null);

        assertThat(resp.getBalances()).isEmpty();
    }

    // ── insertBatch ──────────────────────────────────────────────────────

    @Test
    void insertBatch_givenAllValid_thenInsertedCountMatchesRows_andNoFailures() {
        BatchTransactionsRequest req = batchRequest(3);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchInsertResponse resp = service.insertBatch(USER_ID, req);

        assertThat(resp.getInsertedCount()).isEqualTo(3);
        assertThat(resp.getFailedRows()).isEmpty();
    }

    @Test
    void insertBatch_givenOneRowThrows_thenIsolatesAndCountsAsFailed() {
        BatchTransactionsRequest req = batchRequest(3);
        // Fail the middle row only.
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getReference() != null && t.getReference().equals("ref-1")) {
                throw new RuntimeException("simulated DB constraint");
            }
            return t;
        });

        BatchInsertResponse resp = service.insertBatch(USER_ID, req);

        assertThat(resp.getInsertedCount()).isEqualTo(2);
        assertThat(resp.getFailedRows()).hasSize(1);
        assertThat(resp.getFailedRows().get(0).getRowIndex()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private CreateTransactionRequest txRequest() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setAccountId(UUID.randomUUID());
        req.setEntryType(EntryType.DEBIT);
        req.setAmount(new BigDecimal("123.45"));
        req.setCurrency("USD");
        req.setTransactionDate(LocalDate.of(2026, 5, 23));
        req.setReference("ref");
        req.setDescription("Test");
        return req;
    }

    private Transaction txEntity() {
        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .userId(USER_ID)
                .accountId(UUID.randomUUID())
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .transactionDate(LocalDate.of(2026, 5, 23))
                .source(Source.MANUAL)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private AccountSummary activeAccountSummary(UUID id) {
        return AccountSummary.builder().accountId(id).status(AccountStatus.ACTIVE).currency("USD").build();
    }

    private AccountBalanceAggregate aggregate(UUID accountId, String currency,
                                              BigDecimal credit, BigDecimal debit,
                                              long count, LocalDate lastTxnDate) {
        return new AccountBalanceAggregate() {
            @Override public UUID getAccountId() { return accountId; }
            @Override public String getCurrency() { return currency; }
            @Override public BigDecimal getTotalCredit() { return credit; }
            @Override public BigDecimal getTotalDebit() { return debit; }
            @Override public long getTxnCount() { return count; }
            @Override public LocalDate getLastTxnDate() { return lastTxnDate; }
        };
    }

    private BatchTransactionsRequest batchRequest(int n) {
        BatchTransactionsRequest req = new BatchTransactionsRequest();
        req.setBulkJobId(UUID.randomUUID());
        List<BatchTransactionsRequest.Row> rows = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            BatchTransactionsRequest.Row row = new BatchTransactionsRequest.Row();
            row.setAccountId(UUID.randomUUID());
            row.setEntryType(EntryType.CREDIT);
            row.setAmount(new BigDecimal("1.00"));
            row.setCurrency("USD");
            row.setTransactionDate(LocalDate.of(2026, 5, 23));
            row.setReference("ref-" + i);
            rows.add(row);
        }
        req.setRows(rows);
        return req;
    }
}
