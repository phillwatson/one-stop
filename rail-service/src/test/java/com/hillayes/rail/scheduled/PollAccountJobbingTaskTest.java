package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.Page;
import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.*;
import com.hillayes.nordigen.model.AccountStatus;
import com.hillayes.nordigen.model.AccountSummary;
import com.hillayes.nordigen.model.Balance;
import com.hillayes.nordigen.model.TransactionList;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.UserConsentService;
import com.hillayes.rail.utils.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PollAccountJobbingTaskTest {
    private ServiceConfiguration configuration;
    private UserConsentService userConsentService;
    private AccountRepository accountRepository;
    private AccountBalanceRepository accountBalanceRepository;
    private AccountTransactionRepository accountTransactionRepository;
    private RailAccountService railAccountService;
    private SchedulerFactory scheduler;
    private PollAccountJobbingTask fixture;

    @BeforeEach
    public void init() {
        configuration = mock();
        userConsentService = mock();
        accountRepository = mock();
        accountBalanceRepository = mock();
        accountTransactionRepository = mock();
        railAccountService = mock();
        scheduler = mock();

        // simulate save functionality
        when(accountRepository.save(any())).then(invocation -> {
            Account account = invocation.getArgument(0);
            if (account.getId() == null) {
                account.setId(UUID.randomUUID());
            }
            return account;
        });
        when(accountBalanceRepository.save(any())).then(invocation -> {
            AccountBalance balance = invocation.getArgument(0);
            if (balance.getId() == null) {
                balance.setId(UUID.randomUUID());
            }
            return balance;
        });

        // given: a polling grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        fixture = new PollAccountJobbingTask(configuration, userConsentService,
            accountRepository, accountBalanceRepository, accountTransactionRepository, railAccountService);
    }

    @Test
    public void testGetName() {
        assertEquals("poll-account", fixture.getName());
    }

    @Test
    public void testQueueJob() {
        // given: the jobbing task has been configured
        fixture.taskInitialised(scheduler);

        // when: an account ID is queued for processing
        UUID consentId = UUID.randomUUID();
        String railAccountId = randomAlphanumeric(20);
        fixture.queueJob(consentId, railAccountId);

        // then: the job is passed to the scheduler for queuing
        ArgumentCaptor<PollAccountJobbingTask.Payload> captor =
            ArgumentCaptor.forClass(PollAccountJobbingTask.Payload.class);
        verify(scheduler).addJob(eq(fixture), captor.capture());

        // and: the payload is correct
        assertEquals(consentId, captor.getValue().consentId);
        assertEquals(railAccountId, captor.getValue().railAccountId);
    }

    @Test
    public void testHappyPath_ExistingAccount() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // and: a local account is linked to that rail-account
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .railAccountId(railAccount.id)
            .dateLastPolled(Instant.now().minus(Duration.ofHours(2)))
            .build();
        when(accountRepository.findByRailAccountId(railAccount.id)).thenReturn(Optional.of(account));

        // and: the account has existing transactions - from which transaction poll will start
        AccountTransaction transaction = AccountTransaction.builder()
            .bookingDateTime(Instant.now().minus(Duration.ofDays(2)))
            .build();
        Page<AccountTransaction> page = Page.of(List.of(transaction));
        when(accountTransactionRepository.findByAccountId(eq(account.getId()), any(), anyInt(), anyInt()))
            .thenReturn(page);

        // and: rail-balance records are available
        List<Balance> balances = List.of(
            TestData.mockBalance(),
            TestData.mockBalance()
        );
        when(railAccountService.balances(railAccount.id)).thenReturn(Optional.of(balances));

        // and: rail-transactions records are available
        TransactionList transactions = TestData.mockTransactionList(10, 2);
        when(railAccountService.transactions(eq(railAccount.id), any(), any())).thenReturn(Optional.of(transactions));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-account is retrieved
        verify(railAccountService).get(railAccount.id);

        // and: the local account is retrieved
        verify(accountRepository).findByRailAccountId(railAccount.id);

        // and: the local account is updated with the consent ID
        assertEquals(userConsent.getId(), account.getUserConsentId());

        // and: the account balances are retrieved
        verify(railAccountService).balances(railAccount.id);

        // and: the balances are saved
        verify(accountBalanceRepository, times(balances.size())).save(any());

        // and: the most recent transaction is queried to get start date for poll
        verify(accountTransactionRepository).findByAccountId(eq(account.getId()), any(), anyInt(), anyInt());

        // and: the account transactions are retrieved
        verify(railAccountService).transactions(eq(railAccount.id), any(), any());

        // and: the transactions are saved
        verify(accountTransactionRepository).saveAll(any());

        // and: the local account is updated
        verify(accountRepository).save(account);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testHappyPath_NewAccount() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .institutionId(randomAlphanumeric(20))
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .ownerName(randomAlphanumeric(10))
            .iban(randomAlphanumeric(15))
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // and: also account details are available
        Map<String,Object> details = Map.of(
            "name", randomAlphanumeric(20),
            "cashAccountType", randomAlphanumeric(6),
            "currency", "GBP"
        );
        when(railAccountService.details(railAccount.id)).thenReturn(Optional.of(Map.of("account", details)));

        // and: NO local account is linked to that rail-account
        when(accountRepository.findByRailAccountId(railAccount.id)).thenReturn(Optional.empty());

        // and: NO existing transactions
        when(accountTransactionRepository.findByAccountId(any(), any(), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // and: rail-balance records are available
        List<Balance> balances = List.of(
            TestData.mockBalance(),
            TestData.mockBalance()
        );
        when(railAccountService.balances(railAccount.id)).thenReturn(Optional.of(balances));

        // and: rail-transactions records are available
        TransactionList transactions = TestData.mockTransactionList(10, 2);
        when(railAccountService.transactions(eq(railAccount.id), any(), any())).thenReturn(Optional.of(transactions));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-account is retrieved
        verify(railAccountService).get(railAccount.id);

        // and: the rail-account details are retrieved
        verify(railAccountService).details(railAccount.id);

        // and: the fixture attempts to retrieve the local account
        verify(accountRepository).findByRailAccountId(railAccount.id);

        // and: the account balances are retrieved
        verify(railAccountService).balances(railAccount.id);

        // and: the balances are saved
        verify(accountBalanceRepository, times(balances.size())).save(any());

        // and: the account transactions are retrieved
        verify(railAccountService).transactions(eq(railAccount.id), any(), any());

        // and: the transactions are saved
        verify(accountTransactionRepository).saveAll(any());

        // and: the local account is inserted AND updated
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());
        Account account = captor.getValue();

        // and: the local account is inserted with the consent ID
        assertEquals(userConsent.getId(), account.getUserConsentId());
        assertEquals(userConsent.getId(), account.getUserConsentId());
        assertEquals(userConsent.getUserId(), account.getUserId());
        assertEquals(userConsent.getInstitutionId(), account.getInstitutionId());
        assertEquals(railAccount.id, account.getRailAccountId());
        assertEquals(railAccount.ownerName, account.getOwnerName());
        assertEquals(railAccount.iban, account.getIban());
        assertEquals(details.get("name"), account.getAccountName());
        assertEquals(details.get("cashAccountType"), account.getAccountType());
        assertEquals(details.get("currency"), account.getCurrencyCode());

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testHappyPath_AccountAlreadyPolled() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // and: a local account is linked to that rail-account
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .railAccountId(railAccount.id)
            .dateLastPolled(Instant.now().minus(Duration.ofMinutes(30)))
            .build();
        when(accountRepository.findByRailAccountId(railAccount.id)).thenReturn(Optional.of(account));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-account is retrieved
        verify(railAccountService).get(railAccount.id);

        // and: the local account is retrieved
        verify(accountRepository).findByRailAccountId(railAccount.id);

        // and: NO account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO transactions are queried or saved
        verifyNoInteractions(accountTransactionRepository);

        // and: NO account transactions are retrieved
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO local account is updated
        verify(accountRepository, never()).save(account);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testUserConsentNotFound() {
        // given: a user-consent cannot be found
        UUID userConsentId = UUID.randomUUID();
        when(userConsentService.lockUserConsent(userConsentId)).thenReturn(Optional.empty());

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsentId, railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the fixture attempts to retrieve the user-consent
        verify(userConsentService).lockUserConsent(userConsentId);

        // and: NO rail-account is retrieved
        verifyNoInteractions(railAccountService);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verifyNoInteractions(railAccountService);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verifyNoInteractions(railAccountService);

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "GIVEN" })
    public void testConsentIsNotGiven(ConsentStatus consentStatus) {
        // given: the identified user-consent is not GIVEN
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(consentStatus)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.READY)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: NO rail-account is retrieved
        verifyNoInteractions(railAccountService);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NOaccount balances are retrieved
        verifyNoInteractions(railAccountService);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verifyNoInteractions(railAccountService);

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testNoRailAccountFound() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: NO rail-account associated with that consent can be found
        String railAccountId = randomAlphanumeric(20);
        when(railAccountService.get(railAccountId)).thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccountId);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railAccountService).get(railAccountId);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testNoRailAccountSuspended() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.SUSPENDED)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railAccountService).get(railAccount.id);

        // and: the consent service is called to process suspended requisition
        verify(userConsentService).consentSuspended(userConsent.getId());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testNoRailAccountExpired() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(AccountStatus.EXPIRED)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railAccountService).get(railAccount.id);

        // and: the consent service is called to process expired requisition
        verify(userConsentService).consentExpired(userConsent.getId());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "READY", "EXPIRED", "SUSPENDED" })
    public void testNoRailAccountStatusNotCorrect(AccountStatus accountStatus) {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-account associated with that consent
        AccountSummary railAccount = AccountSummary.builder()
            .id(randomAlphanumeric(20))
            .status(accountStatus)
            .build();
        when(railAccountService.get(railAccount.id)).thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.id);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railAccountService).get(railAccount.id);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the consent service is NOT called to process expired requisition
        verify(userConsentService, never()).consentExpired(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }
}
