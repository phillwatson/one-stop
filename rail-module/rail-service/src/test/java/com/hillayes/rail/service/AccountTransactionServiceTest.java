package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AccountTransactionServiceTest {
    @InjectMock
    AccountService accountService;

    @InjectMock
    AccountTransactionRepository accountTransactionRepository;

    @Inject
    AccountTransactionService fixture;

    @Test
    public void testGetTransactions_WithAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .limit(23)
            .map(n -> TestData.mockAccountTransaction(account))
            .toList();
        when(accountTransactionRepository.findByAccountId(eq(account.getId()), any(), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(2);
                int pageSize = invocation.getArgument(3);
                return Page.of(transactions, pageIndex, pageSize);
            });

        // when: the transactions are requested
        Page<AccountTransaction> result =
            fixture.getTransactions(account.getUserId(), account.getId(), 2, 5);

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
        verify(accountTransactionRepository).findByAccountId(eq(account.getId()), any(), anyInt(), anyInt());

        // and: the account is verified
        verify(accountService).getAccount(account.getId());
    }

    @Test
    public void testGetTransactions_WithWrongAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountTransaction(account))
            .limit(23)
            .toList();
        when(accountTransactionRepository.findByAccountId(eq(account.getId()), any(), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(2);
                int pageSize = invocation.getArgument(3);
                return Page.of(transactions, pageIndex, pageSize);
            });

        // when: the transactions are requested - with wrong account ID
        // then: an NotFoundException is thrown
        assertThrows(NotFoundException.class, () ->
            fixture.getTransactions(account.getUserId(), UUID.randomUUID(), 2, 5)
        );
    }

    @Test
    public void testGetTransactions_WithNoAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountTransaction(account))
            .limit(23)
            .toList();
        when(accountTransactionRepository.findByUserId(eq(account.getUserId()), any(), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(2);
                int pageSize = invocation.getArgument(3);
                return Page.of(transactions, pageIndex, pageSize);
            });

        // when: the transactions are requested - without account ID
        Page<AccountTransaction> result =
            fixture.getTransactions(account.getUserId(), null, 2, 5);

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
        verify(accountTransactionRepository).findByUserId(eq(account.getUserId()), any(), anyInt(), anyInt());

        // and: the account is NOT verified
        verify(accountService, never()).getAccount(account.getId());
    }

    @Test
    public void testListTransactions_WithAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountTransaction(account))
            .limit(23)
            .toList();
        when(accountTransactionRepository.findByAccountAndDateRange(eq(account.getId()), any(), any()))
            .thenReturn(transactions);

        // when: the transactions are requested - with account ID
        List<AccountTransaction> result =
            fixture.listTransactions(account.getUserId(), account.getId(),
                LocalDate.now().minusDays(20), LocalDate.now());

        // then: the transactions are returned
        assertNotNull(result);

        // and: the accounts are returned
        assertEquals(transactions.size(), result.size());

        // and: the transactions are retrieved by account ID
        verify(accountTransactionRepository).findByAccountAndDateRange(eq(account.getId()), any(), any());

        // and: the account is verified
        verify(accountService).getAccount(account.getId());
    }

    @Test
    public void testListTransactions_WithWrongAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountTransaction(account))
            .limit(23)
            .toList();
        when(accountTransactionRepository.findByAccountAndDateRange(eq(account.getId()), any(), any()))
            .thenReturn(transactions);

        // when: the transactions are requested - with account ID
        assertThrows(NotFoundException.class, () ->
            fixture.listTransactions(account.getUserId(), UUID.randomUUID(),
                LocalDate.now().minusDays(20), LocalDate.now())
        );
    }

    @Test
    public void testListTransactions_WithNoAccountId() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountService.getAccount(account.getId())).thenReturn(Optional.of(account));

        // and: a collection of transactions
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountTransaction(account))
            .limit(23)
            .toList();
        when(accountTransactionRepository.findByUserAndDateRange(eq(account.getUserId()), any(), any()))
            .thenReturn(transactions);

        // when: the transactions are requested - with account ID
        List<AccountTransaction> result =
            fixture.listTransactions(account.getUserId(), null,
                LocalDate.now().minusDays(20), LocalDate.now());

        // then: the transactions are returned
        assertNotNull(result);

        // and: the accounts are returned
        assertEquals(transactions.size(), result.size());

        // and: the transactions are retrieved by user ID
        verify(accountTransactionRepository).findByUserAndDateRange(eq(account.getUserId()), any(), any());

        // and: the account is NOT verified
        verify(accountService, never()).getAccount(account.getId());
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
}
