package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.TransactionFilter;
import com.hillayes.rail.utils.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AccountTransactionServiceTest {
    private final AccountService accountService = mock();
    private final CategoryService categoryService = mock();
    private final AccountTransactionRepository accountTransactionRepository = mock();

    private final AccountTransactionService fixture = new AccountTransactionService(
        accountService,
        categoryService,
        accountTransactionRepository
    );

    @Test
    public void testGetTransactions_WithAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getUserId(), account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions belonging to that account
        List<AccountTransaction> transactions = mockTransactions(account);

        // and: a filter containing the account ID
        TransactionFilter filter = TransactionFilter.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .build();

        // when: the transactions are requested
        Page<AccountTransaction> result =
            fixture.getTransactions(filter, 2, 5);

        // then: the transactions are returned
        assertNotNull(result);

        // and: the page matches request
        assertNotNull(result.getContent());
        assertEquals(5, result.getContent().size());
        assertEquals(2, result.getPageIndex());
        assertEquals(5, result.getPageSize());
        assertEquals(transactions.size(), result.getTotalCount());
        assertEquals(5, result.getTotalPages());

        // and: the transactions are retrieved by account ID
        verify(accountTransactionRepository).findByFilter(eq(filter), anyInt(), anyInt());

        // and: the account is verified
        verify(accountService).getAccount(account.getUserId(), account.getId());
    }

    @Test
    public void testGetTransactions_WithWrongAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getUserId(), account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions belonging to that account
        mockTransactions(account);

        // when: the transactions are requested - with wrong account ID
        // then: an NotFoundException is thrown
        TransactionFilter filter = TransactionFilter.builder()
            .userId(account.getUserId())
            .accountId(UUID.randomUUID())
            .build();
        assertThrows(NotFoundException.class, () ->
            fixture.getTransactions(filter, 2, 5)
        );
    }

    @Test
    public void testGetTransactions_WithNoAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getUserId(), account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions belonging to that account
        List<AccountTransaction> transactions = mockTransactions(account);

        // when: the transactions are requested - without account ID (or any other filter)
        Page<AccountTransaction> result =
            fixture.getTransactions(null, 2, 5);

        // then: the transactions are returned
        assertNotNull(result);

        // and: the page matches request
        assertNotNull(result.getContent());
        assertEquals(5, result.getContent().size());
        assertEquals(2, result.getPageIndex());
        assertEquals(5, result.getPageSize());
        assertEquals(transactions.size(), result.getTotalCount());
        assertEquals(5, result.getTotalPages());

        // and: the transactions are retrieved by user ID
        verify(accountTransactionRepository).findByFilter(any(), anyInt(), anyInt());

        // and: the account is NOT verified
        verify(accountService, never()).getAccount(account.getUserId(), account.getId());
    }

    @Test
    public void testGetTransaction() {
        // given: a transaction exists for an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        AccountTransaction transaction = TestData.mockAccountTransaction(account);
        when(accountTransactionRepository.findByIdOptional(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        // when: the transaction is requested
        Optional<AccountTransaction> result = fixture.getTransaction(transaction.getId());

        // then: the transaction is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(transaction, result.get());
    }

    @Test
    public void testGetTransaction_NotFound() {
        // given: an unknown transaction ID
        UUID transactionId = UUID.randomUUID();
        when(accountTransactionRepository.findByIdOptional(transactionId))
            .thenReturn(Optional.empty());

        // when: the transaction is requested
        Optional<AccountTransaction> result = fixture.getTransaction(transactionId);

        // then: NO transaction is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private List<AccountTransaction> mockTransactions(Account account) {
        // and: a collection of transactions belonging to that account
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .limit(23)
            .map(n -> TestData.mockAccountTransaction(account))
            .toList();
        when(accountTransactionRepository.findByFilter(any(), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(1);
                int pageSize = invocation.getArgument(2);
                return Page.of(transactions, pageIndex, pageSize);
            });

        return transactions;
    }
}
