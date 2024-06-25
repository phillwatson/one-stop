package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.Page;
import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.service.UserConsentService;
import com.hillayes.rail.utils.TestApiData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class PollAccountJobbingTaskTest {
    @Mock
    UserConsentService userConsentService;
    @Mock
    AccountRepository accountRepository;
    @Mock
    AccountBalanceRepository accountBalanceRepository;
    @Mock
    AccountTransactionRepository accountTransactionRepository;
    @Mock
    RailProviderFactory railProviderFactory;
    @Mock
    RailProviderApi railProviderApi;
    @Mock
    SchedulerFactory scheduler;
    @Mock
    ServiceConfiguration configuration;

    @InjectMocks
    PollAccountJobbingTask fixture;

    @BeforeEach
    public void init() {
        openMocks(this);

        when(railProviderFactory.get(any())).thenReturn(railProviderApi);
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

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
        ServiceConfiguration configuration = mock();
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        RailProviderFactory railProviderFactory = mock();
        when(railProviderFactory.get(any())).thenReturn(railProviderApi);
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
        assertEquals(consentId, captor.getValue().consentId());
        assertEquals(railAccountId, captor.getValue().railAccountId());
    }

    @Test
    public void testHappyPath_ExistingAccount() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount();
        when(railProviderApi.getAccount(railAgreement, railAccount.getId()))
            .thenReturn(Optional.of(railAccount));

        // and: a local account is linked to that rail-account
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .railAccountId(railAccount.getId())
            .dateLastPolled(Instant.now().minus(Duration.ofHours(2)))
            .build();
        when(accountRepository.findByRailAccountId(railAccount.getId())).thenReturn(Optional.of(account));

        // and: the account has existing transactions - from which transaction poll will start
        AccountTransaction transaction = AccountTransaction.builder()
            .bookingDateTime(Instant.now().minus(Duration.ofDays(2)))
            .build();
        Page<AccountTransaction> page = Page.of(List.of(transaction));
        when(accountTransactionRepository.findByFilter(any(), anyInt(), anyInt()))
            .thenReturn(page);

        // and: rail-transactions records are available
        List<RailTransaction> transactions = TestApiData.mockTransactionList(10);
        when(railProviderApi.listTransactions(eq(railAgreement), eq(railAccount.getId()), any())).thenReturn(transactions);

        // and: balance records already exist for earlier dates
        when(accountBalanceRepository.findMostRecentByAccountId(account.getId())).thenReturn(
            Optional.of(AccountBalance.builder().referenceDate(Instant.now().minus(Duration.ofDays(1))).build())
        );

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the rail-account is retrieved
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: the local account is retrieved
        verify(accountRepository).findByRailAccountId(railAccount.getId());

        // and: the local account is updated with the consent ID
        assertEquals(userConsent.getId(), account.getUserConsentId());

        // and: the balances are saved
        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceRepository).save(balanceCaptor.capture());
        AccountBalance accountBalance = balanceCaptor.getValue();

        // and: the balance is taken from the rail account data
        assertEquals(account.getId(), accountBalance.getAccountId());
        assertEquals(railAccount.getBalance().getType(), accountBalance.getBalanceType());
        assertEquals(railAccount.getBalance().getDateTime(), accountBalance.getReferenceDate());
        assertEquals(railAccount.getBalance().getAmount(), accountBalance.getAmount());

        // and: the most recent transaction is queried to get start date for poll
        verify(accountTransactionRepository).findByFilter(any(), anyInt(), anyInt());

        // and: the account transactions are retrieved
        verify(railProviderApi).listTransactions(eq(railAgreement), eq(railAccount.getId()), any());

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
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .status(ConsentStatus.GIVEN)
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount();
        when(railProviderApi.getAccount(railAgreement, railAccount.getId()))
            .thenReturn(Optional.of(railAccount));

        // and: NO local account is linked to that rail-account
        when(accountRepository.findByRailAccountId(railAccount.getId())).thenReturn(Optional.empty());

        // and: NO existing transactions
        when(accountTransactionRepository.findByFilter(any(), anyInt(), anyInt()))
            .thenReturn(Page.empty());

        // and: rail-transactions records are available
        List<RailTransaction> transactions = TestApiData.mockTransactionList(10);
        when(railProviderApi.listTransactions(eq(railAgreement), eq(railAccount.getId()), any())).thenReturn(transactions);

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the rail-account is retrieved
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: the fixture attempts to retrieve the local account
        verify(accountRepository).findByRailAccountId(railAccount.getId());

        // and: the balances are saved
        verify(accountBalanceRepository).save(any());

        // and: the account transactions are retrieved
        verify(railProviderApi).listTransactions(eq(railAgreement), eq(railAccount.getId()), any());

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
        assertEquals(railAccount.getId(), account.getRailAccountId());
        assertEquals(railAccount.getOwnerName(), account.getOwnerName());
        assertEquals(railAccount.getIban(), account.getIban());
        assertEquals(railAccount.getName(), account.getAccountName());
        assertEquals(railAccount.getAccountType(), account.getAccountType());
        assertEquals(railAccount.getCurrency().getCurrencyCode(), account.getCurrency().getCurrencyCode());

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

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount();
        when(railProviderApi.getAccount(railAgreement, railAccount.getId())).thenReturn(Optional.of(railAccount));

        // and: a local account is linked to that rail-account
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .railAccountId(railAccount.getId())
            .dateLastPolled(Instant.now().minus(Duration.ofMinutes(30)))
            .build();
        when(accountRepository.findByRailAccountId(railAccount.getId())).thenReturn(Optional.of(account));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the rail-account is retrieved
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: the local account is retrieved
        verify(accountRepository).findByRailAccountId(railAccount.getId());

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO transactions are queried or saved
        verifyNoInteractions(accountTransactionRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

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
        RailAccount railAccount = TestApiData.mockAccount();

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsentId, railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the fixture attempts to retrieve the user-consent
        verify(userConsentService).lockUserConsent(userConsentId);

        // and: NO rail-account is retrieved
        verifyNoInteractions(railProviderApi);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO account balances are retrieved
        verifyNoInteractions(railProviderApi);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verifyNoInteractions(railProviderApi);

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
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), UUID.randomUUID().toString());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: NO rail-account is retrieved
        verifyNoInteractions(railProviderApi);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NOaccount balances are retrieved
        verifyNoInteractions(railProviderApi);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verifyNoInteractions(railProviderApi);

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
    public void testNoRailAgreementFound() {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: NO rail-agreement associated with that consent
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), randomAlphanumeric(20));
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the fixture attempts to retrieve the rail-agreement
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railProviderApi, never()).getAccount(any(), any());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

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
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: NO rail-account associated with that consent can be found
        String railAccountId = randomAlphanumeric(20);
        when(railProviderApi.getAccount(railAgreement, railAccountId))
            .thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccountId);
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railProviderApi).getAccount(railAgreement, railAccountId);

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

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
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount(RailAccountStatus.SUSPENDED);
        when(railProviderApi.getAccount(railAgreement, railAccount.getId()))
            .thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: the consent service is called to process suspended requisition
        verify(userConsentService).consentSuspended(userConsent.getId());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

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
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount(RailAccountStatus.EXPIRED);
        when(railProviderApi.getAccount(railAgreement, railAccount.getId()))
            .thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: the consent service is called to process expired requisition
        verify(userConsentService).consentExpired(userConsent.getId());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

        // and: NO transactions are saved
        verifyNoInteractions(accountTransactionRepository);

        // and: the consent service is NOT called to process suspended requisition
        verify(userConsentService, never()).consentSuspended(any());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "READY", "EXPIRED", "SUSPENDED" })
    public void testNoRailAccountStatusNotCorrect(RailAccountStatus accountStatus) {
        // given: an identified user-consent ready to be polled
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .agreementId(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .build();
        when(userConsentService.lockUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a rail-agreement associated with that consent
        RailAgreement railAgreement = RailAgreement.builder().build();
        when(railProviderApi.getAgreement(userConsent.getAgreementId()))
            .thenReturn(Optional.of(railAgreement));

        // and: a rail-account associated with that consent
        RailAccount railAccount = TestApiData.mockAccount(accountStatus);
        when(railProviderApi.getAccount(railAgreement, railAccount.getId()))
            .thenReturn(Optional.of(railAccount));

        // when: the fixture is called to process the user-consent and account
        PollAccountJobbingTask.Payload payload = new PollAccountJobbingTask.Payload(userConsent.getId(), railAccount.getId());
        TaskContext<PollAccountJobbingTask.Payload> context = new TaskContext<>(payload);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).lockUserConsent(userConsent.getId());

        // and: the rail-agreement is retrieved
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: the fixture attempts to retrieve the rail-account
        verify(railProviderApi).getAccount(railAgreement, railAccount.getId());

        // and: NO local account is retrieved
        verifyNoInteractions(accountRepository);

        // and: NO balances are saved
        verifyNoInteractions(accountBalanceRepository);

        // and: NO account transactions are retrieved
        verify(railProviderApi, never()).listTransactions(any(), any(), any());

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
