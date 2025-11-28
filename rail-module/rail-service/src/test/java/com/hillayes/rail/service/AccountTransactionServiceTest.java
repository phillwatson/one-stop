package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.TransactionFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.stream.Stream;

import static com.hillayes.rail.utils.TestData.mockAccount;
import static com.hillayes.rail.utils.TestData.mockAccountTransaction;
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
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
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
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
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
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
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
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
        AccountTransaction transaction = mockAccountTransaction(account);
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

    @Test
    public void testUpdateTransaction() {
        // given: a user ID and an account
        UUID userId = UUID.randomUUID();
        Account account = mockAccount(userId, UUID.randomUUID());

        when(accountTransactionRepository.save(any())).then( invocation ->
            invocation.getArgument(0)
        );

        Stream.of(Boolean.TRUE, Boolean.FALSE, null).forEach(reconciled -> {
            Stream.of("modified notes", null).forEach(notes -> {
                // and: a transaction exists
                AccountTransaction transaction = spy(mockAccountTransaction(account));
                when(accountTransactionRepository.findByIdOptional(transaction.getId()))
                    .thenReturn(Optional.of(transaction));

                // when: the transaction is updated
                AccountTransaction result = fixture.updateTransaction(userId, transaction.getId(),
                    Optional.ofNullable(reconciled),
                    Optional.ofNullable(notes));

                // then: the updated transaction is returned
                assertNotNull(result);

                // and: the updated are applied
                if (reconciled != null) {
                    verify(transaction).setReconciled(reconciled);
                } else {
                    verify(transaction, never()).setReconciled(anyBoolean());
                }
                if (notes != null) {
                    verify(transaction).setNotes(notes);
                } else {
                    verify(transaction, never()).setNotes(anyString());
                }

                // and: the update transaction is saved
                verify(accountTransactionRepository).save(transaction);
            });
        });
    }

    @Test
    public void testUpdateTransaction_NotFound() {
        // given: a user iD
        UUID userId = UUID.randomUUID();

        // and: an unknown transaction ID
        UUID transactionId = UUID.randomUUID();
        when(accountTransactionRepository.findByIdOptional(transactionId))
            .thenReturn(Optional.empty());

        Optional<Boolean> reconciled = Optional.of(Boolean.TRUE);
        Optional<String> notes = Optional.of("modified notes");

        // when: the transaction is updated
        // then: an exception is thrown
        assertThrows(NotFoundException.class, () ->
            fixture.updateTransaction(userId, transactionId, reconciled, notes)
        );

        // and: NO transaction is updated
        verify(accountTransactionRepository, never()).save(any());
    }

    @Test
    public void testUpdateTransaction_WrongUser() {
        // given: a user iD
        UUID userId = UUID.randomUUID();

        // given: a transaction exists for another user's account
        Account account = mockAccount(UUID.randomUUID(), UUID.randomUUID());
        AccountTransaction transaction = mockAccountTransaction(account);
        when(accountTransactionRepository.findByIdOptional(transaction.getId()))
            .thenReturn(Optional.of(transaction));

        Optional<Boolean> reconciled = Optional.of(Boolean.TRUE);
        Optional<String> notes = Optional.of("modified notes");

        // when: the transaction is updated
        // then: an exception is thrown
        assertThrows(NotFoundException.class, () ->
            fixture.updateTransaction(userId, transaction.getId(), reconciled, notes)
        );

        // and: NO transaction is updated
        verify(accountTransactionRepository, never()).save(any());
    }

    @Test
    public void testBatchReconciliationUpdate() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: a mixed batch of updates
        Map<UUID, Boolean> updates = Map.of(
            UUID.randomUUID(), true,
            UUID.randomUUID(), false,
            UUID.randomUUID(), true,
            UUID.randomUUID(), false,
            UUID.randomUUID(), true
        );

        // and: the repository can perform update
        when(accountTransactionRepository.update(anyString(), anyMap())).thenReturn(10);

        // when: the updates are passed to service
        fixture.batchReconciliationUpdate(userId, updates);

        // then: the repository is called twice to perform the updates
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(accountTransactionRepository, times(2))
            .update(queryCaptor.capture(), paramsCaptor.capture());

        // and: the query parameters are as expected
        Iterator<Object[]> params = paramsCaptor.getAllValues().iterator();
        queryCaptor.getAllValues().forEach(query -> {
            Object[] actualParams = params.next();

            assertEquals(userId, actualParams[0]);
            if (query.contains("reconciled = true")) {
                // true value count
                assertEquals(3, ((List<?>)actualParams[1]).size());
            } else {
                // false value count
                assertEquals(2, ((List<?>)actualParams[1]).size());
            }
        });
    }

    private List<AccountTransaction> mockTransactions(Account account) {
        // and: a collection of transactions belonging to that account
        List<AccountTransaction> transactions = Stream.iterate(1, (n) -> n + 1)
            .limit(23)
            .map(n -> mockAccountTransaction(account))
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
