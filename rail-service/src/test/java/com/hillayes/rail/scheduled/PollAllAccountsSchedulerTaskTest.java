package com.hillayes.rail.scheduled;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PollAllAccountsSchedulerTaskTest {
    private AccountRepository accountRepository;

    private PollAccountJobbingTask pollAccountJobbingTask;

    private PollAllAccountsSchedulerTask fixture;

    @BeforeEach
    public void init() {
        accountRepository = mock();
        pollAccountJobbingTask = mock();

        fixture = new PollAllAccountsSchedulerTask(accountRepository, pollAccountJobbingTask);
    }

    @Test
    public void testGetName() {
        assertEquals("poll-all-accounts", fixture.getName());
    }

    @Test
    public void testRun_WithAccounts() {
        // given: a collection of accounts
        List<Account> accounts = List.of(
            Account.builder().id(UUID.randomUUID()).build(),
            Account.builder().id(UUID.randomUUID()).build(),
            Account.builder().id(UUID.randomUUID()).build()
        );

        // and: the repository returns the accounts
        when(accountRepository.findAll()).thenReturn(accounts);

        // when: the fixture is invoked
        fixture.run();

        // then: a poll-account task is queued for each account
        accounts.forEach(account ->
            verify(pollAccountJobbingTask).queueJob(account.getId())
        );
    }

    @Test
    public void testRun_WithNoAccounts() {
        // given: an empty collection of accounts
        List<Account> accounts = List.of();

        // and: the repository returns the empty list
        when(accountRepository.findAll()).thenReturn(accounts);

        // when: the fixture is invoked
        fixture.run();

        // then: NO poll-account task is queued for any account
        verify(pollAccountJobbingTask, never()).queueJob(any());
    }
}
