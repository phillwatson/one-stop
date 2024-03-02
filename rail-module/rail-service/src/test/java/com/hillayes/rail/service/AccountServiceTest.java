package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AccountServiceTest {
    @InjectMock
    AccountRepository accountRepository;
    @InjectMock
    AccountBalanceRepository accountBalanceRepository;
    @InjectMock
    UserConsentService userConsentService;

    @Inject
    AccountService fixture;

    @Test
    public void testGetAccounts() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: a collection of accounts
        List<Account> accounts = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccount(userId, UUID.randomUUID()))
            .limit(10)
            .toList();

        // and: the account repository returns the collection of accounts
        when(accountRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                int pageIndex = invocation.getArgument(1);
                int pageSize = invocation.getArgument(2);
                return Page.of(accounts, pageIndex, pageSize);
            });

        // when: the accounts are requested
        Page<Account> page = fixture.getAccounts(userId, 0, 5);

        // then: the page is returned
        assertNotNull(page);

        // and: the accounts are in the page
        assertFalse(page.getContent().isEmpty());
        assertEquals(10, page.getTotalCount());

        // and: the parameters were passed to the repository
        verify(accountRepository).findByUserId(eq(userId), eq(0), eq(5));
    }

    @Test
    public void testGetAccounts_NonFound() {
        // given: a user id
        UUID userId = UUID.randomUUID();

        // and: the account repository returns an empty collection of accounts
        when(accountRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // when: the accounts are requested
        Page<Account> page = fixture.getAccounts(userId, 0, 5);

        // then: the page is returned
        assertNotNull(page);

        // and: the page is empty
        assertEquals(0, page.getContentSize());
        assertEquals(0, page.getTotalCount());

        // and: the parameters were passed to the repository
        verify(accountRepository).findByUserId(eq(userId), eq(0), eq(5));
    }

    @Test
    public void testGetAccountsByUserConsent() {
        // given: a consent record
        UserConsent userConsent = TestData.mockUserConsent(UUID.randomUUID());

        // and: a collection of accounts
        List<Account> accounts = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccount(userConsent.getUserId(), userConsent.getId()))
            .limit(5)
            .toList();
        when(accountRepository.findByUserConsentId(userConsent.getId()))
            .thenReturn(accounts);

        // when: the service is called
        List<Account> result = fixture.getAccountsByUserConsent(userConsent);

        // then: the accounts are returned
        assertNotNull(result);
        assertEquals(accounts.size(), result.size());
    }

    @Test
    public void testGetAccountsByUserConsent_NonFound() {
        // given: a consent record
        UserConsent userConsent = TestData.mockUserConsent(UUID.randomUUID());

        // and: no accounts are found
        when(accountRepository.findByUserConsentId(userConsent.getId()))
            .thenReturn(List.of());

        // when: the service is called
        List<Account> result = fixture.getAccountsByUserConsent(userConsent);

        // then: the result is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAccount_NotFound() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: an unknown account ID
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdOptional(accountId))
            .thenReturn(Optional.empty());

        // and: NO associated active consent
        when(userConsentService.getUserConsent(any(), any()))
            .thenReturn(Optional.empty());

        // when: the account is requested
        Optional<Account> result = fixture.getAccount(userId, accountId);

        // then: the account is NOT returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // and: no attempt to retrieve the consent is made
        verifyNoInteractions(userConsentService);
    }

    @Test
    public void testGetAccount_HappyPath() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: an associated active consent
        UserConsent userConsent = TestData.mockUserConsent(account.getUserId(), (c) -> c.id(account.getUserConsentId()));
        when(userConsentService.getUserConsent(account.getUserId(), account.getInstitutionId()))
            .thenReturn(Optional.of(userConsent));

        // when: the account is requested
        Optional<Account> result = fixture.getAccount(account.getUserId(), account.getId());

        // then: the account is returned
        assertNotNull(result);
        assertTrue(result.isPresent());

        // and: the account is the expected one
        assertEquals(account, result.get());
    }

    @Test
    public void testGetAccount_ConsentExpired() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // and: NO associated active consent
        when(userConsentService.getUserConsent(account.getUserId(), account.getInstitutionId()))
            .thenReturn(Optional.empty());

        // when: the account is requested
        Optional<Account> result = fixture.getAccount(account.getUserId(), account.getId());

        // then: the account is NOT returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetMostRecentBalance() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());

        // and: a collection of balance record on the same date
        List<AccountBalance> balances = Stream.iterate(1, (n) -> n + 1)
            .map(n -> TestData.mockAccountBalance(account))
            .limit(5)
            .toList();
        when(accountBalanceRepository.findFirstByAccountIdOrderByReferenceDateDesc(account.getId()))
            .thenReturn(Optional.of(balances.get(0)));
        when(accountBalanceRepository.findByAccountIdAndReferenceDate(account.getId(), balances.get(0).getReferenceDate()))
            .thenReturn(balances);

        // when: the most recent balance is requested
        List<AccountBalance> result = fixture.getMostRecentBalance(account);

        // then: the balances are returned
        assertNotNull(result);
        assertEquals(balances.size(), result.size());
    }

    @Test
    public void testGetMostRecentBalance_NonFound() {
        // given: an account
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());

        // and: NO balance records
        when(accountBalanceRepository.findFirstByAccountIdOrderByReferenceDateDesc(account.getId()))
            .thenReturn(Optional.empty());

        // when: the most recent balance is requested
        List<AccountBalance> result = fixture.getMostRecentBalance(account);

        // then: the balances are returned
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // and: an attempt to retrieve balances by date order is made
        verify(accountBalanceRepository).findFirstByAccountIdOrderByReferenceDateDesc(any());

        // and: no attempt to retrieve the balances on reference date is made
        verify(accountBalanceRepository, never()).findByAccountIdAndReferenceDate(any(), any());
    }

    @Test
    public void testDeleteAccount_HappyPath() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: an account
        Account account = TestData.mockAccount(userId, UUID.randomUUID());
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // when: the account is deleted
        boolean result = fixture.deleteAccount(userId, account.getId());

        // then: the result is true
        assertTrue(result);

        // and: the account is deleted
        verify(accountRepository).delete(account);
    }

    @Test
    public void testDeleteAccount_WrongUser() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: an account belonging to another user
        Account account = TestData.mockAccount(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findByIdOptional(account.getId()))
            .thenReturn(Optional.of(account));

        // when: the account is deleted
        boolean result = fixture.deleteAccount(userId, account.getId());

        // then: the result is false
        assertFalse(result);

        // and: the account is NOT deleted
        verify(accountRepository, never()).delete(account);
    }

    @Test
    public void testDeleteAccount_NotFound() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: an unknown account ID
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdOptional(accountId))
            .thenReturn(Optional.empty());

        // when: the account is deleted
        boolean result = fixture.deleteAccount(userId, accountId);

        // then: the result is false
        assertFalse(result);

        // and: no attempt to delete the account is made
        verify(accountRepository, never()).delete(any());
    }
}
