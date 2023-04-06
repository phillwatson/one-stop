package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.Balance;
import com.hillayes.rail.model.CurrencyAmount;
import com.hillayes.rail.model.TransactionDetail;
import com.hillayes.rail.model.TransactionList;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.service.RailAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextFloat;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class PollAccountJobbingTaskTest {
    private ServiceConfiguration configuration;
    private UserConsentRepository userConsentRepository;
    private AccountRepository accountRepository;
    private AccountBalanceRepository accountBalanceRepository;
    private AccountTransactionRepository accountTransactionRepository;
    private RailAccountService railAccountService;
    private SchedulerFactory scheduler;
    private PollAccountJobbingTask fixture;

    @BeforeEach
    public void init() {
        configuration = mock();
        userConsentRepository = mock();
        accountRepository = mock();
        accountBalanceRepository = mock();
        accountTransactionRepository = mock();
        railAccountService = mock();
        scheduler = mock();

        // simulate save functionality
        when(accountBalanceRepository.save(any())).then(invocation -> {
            AccountBalance balance = invocation.getArgument(0);
            balance.setId(UUID.randomUUID());
            return balance;
        });

        fixture = new PollAccountJobbingTask(configuration, userConsentRepository,
            accountRepository, accountBalanceRepository, accountTransactionRepository, railAccountService);
    }

    @Test
    public void testGetName() {
        assertEquals("poll-account", fixture.getName());
    }

    @Test
    public void testQueueJob() {
        // given: the jobbing task has been configured
        fixture.taskScheduled(scheduler);

        // when: an account ID is queued for processing
        UUID accountId = UUID.randomUUID();
        fixture.queueJob(accountId);

        // then: the job is passed to the scheduler for queuing
        verify(scheduler).addJob(fixture, accountId);
    }

    @Test
    public void testAccept_WithNewAccount() {
        // given: a UserConsent, associated with the account to be processed, is still active
        UserConsent userConsent = mockUserConsent(ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: an account to be processed for the first time
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .dateLastPolled(null) // never been processed before
            .userConsentId(userConsent.getId())
            .userId(userConsent.getUserId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
        when(accountRepository.findById(any())).thenReturn(Optional.of(account));

        // and: the account has no existing transactions
        when(accountTransactionRepository.findByAccountId(any(), any())).thenReturn(List.of());

        // and: the rail account balances are available
        List<Balance> balances = List.of(mockBalance(), mockBalance());
        when(railAccountService.balances(any())).thenReturn(Optional.of(balances));

        // and: the rail account transaction are available
        TransactionList railTransactions = mockTransactionList();
        when(railAccountService.transactions(any(), any(), any())).thenReturn(Optional.of(railTransactions));

        // and: a grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        // when: the fixture is called to process the account
        fixture.accept(account.getId());

        // then: the account is retrieved
        verify(accountRepository).findById(account.getId());

        // and: the user-consent is retrieved
        verify(userConsentRepository).findById(account.getUserConsentId());

        // and: the rail account balances are retrieved
        verify(railAccountService).balances(account.getRailAccountId());

        // and: each balance record is saved
        verify(accountBalanceRepository, times(balances.size())).save(any());

        // and: the account's local transactions are searched - to determine date of last transaction
        verify(accountTransactionRepository).findByAccountId(eq(account.getId()), any());

        // and: the rail account transaction are retrieved - using the permitted max history
        LocalDate startDate = LocalDate.now().minusDays(userConsent.getMaxHistory());
        verify(railAccountService).transactions(account.getRailAccountId(), startDate, LocalDate.now());

        // and: the retrieved transactions are saved
        verify(accountTransactionRepository).saveAll(any());

        // and: the account's last-polled date is updated and saved
        assertNotNull(account.getDateLastPolled());
        verify(accountRepository).save(account);
    }

    @Test
    public void testAccept_NoBalanceRecords() {
        // given: a UserConsent, associated with the account to be processed, is still active
        UserConsent userConsent = mockUserConsent(ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: an account to be processed for the first time
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .dateLastPolled(null) // never been processed before
            .userConsentId(userConsent.getId())
            .userId(userConsent.getUserId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
        when(accountRepository.findById(any())).thenReturn(Optional.of(account));

        // and: the account has no existing transactions
        when(accountTransactionRepository.findByAccountId(any(), any())).thenReturn(List.of());

        // and: NO rail account balances are available
        List<Balance> balances = List.of();
        when(railAccountService.balances(any())).thenReturn(Optional.of(balances));

        // and: the rail account transaction are available
        TransactionList railTransactions = mockTransactionList();
        when(railAccountService.transactions(any(), any(), any())).thenReturn(Optional.of(railTransactions));

        // and: a grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        // when: the fixture is called to process the account
        fixture.accept(account.getId());

        // then: the account is retrieved
        verify(accountRepository).findById(account.getId());

        // and: the user-consent is retrieved
        verify(userConsentRepository).findById(account.getUserConsentId());

        // and: the rail account balances are retrieved
        verify(railAccountService).balances(account.getRailAccountId());

        // and: NO balance record is saved
        verify(accountBalanceRepository, never()).save(any());

        // and: the account's local transactions are searched - to determine date of last transaction
        verify(accountTransactionRepository).findByAccountId(eq(account.getId()), any());

        // and: the rail account transaction are retrieved - using the permitted max history
        LocalDate startDate = LocalDate.now().minusDays(userConsent.getMaxHistory());
        verify(railAccountService).transactions(account.getRailAccountId(), startDate, LocalDate.now());

        // and: the retrieved transactions are saved
        verify(accountTransactionRepository).saveAll(any());

        // and: the account's last-polled date is updated and saved
        assertNotNull(account.getDateLastPolled());
        verify(accountRepository).save(account);
    }

    @Test
    public void testAccept_NoNewTransaction() {
        // given: a UserConsent, associated with the account to be processed, is still active
        UserConsent userConsent = mockUserConsent(ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: an account to be processed for the first time
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .dateLastPolled(null) // never been processed before
            .userConsentId(userConsent.getId())
            .userId(userConsent.getUserId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
        when(accountRepository.findById(any())).thenReturn(Optional.of(account));

        // and: the account has no existing transactions
        when(accountTransactionRepository.findByAccountId(any(), any())).thenReturn(List.of());

        // and: the rail account balances are available
        List<Balance> balances = List.of(mockBalance(), mockBalance());
        when(railAccountService.balances(any())).thenReturn(Optional.of(balances));

        // and: the rail account transaction are available
        TransactionList railTransactions = TransactionList.NULL_LIST;
        when(railAccountService.transactions(any(), any(), any())).thenReturn(Optional.of(railTransactions));

        // and: a grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        // when: the fixture is called to process the account
        fixture.accept(account.getId());

        // then: the account is retrieved
        verify(accountRepository).findById(account.getId());

        // and: the user-consent is retrieved
        verify(userConsentRepository).findById(account.getUserConsentId());

        // and: the rail account balances are retrieved
        verify(railAccountService).balances(account.getRailAccountId());

        // and: each balance record is saved
        verify(accountBalanceRepository, times(balances.size())).save(any());

        // and: the account's local transactions are searched - to determine date of last transaction
        verify(accountTransactionRepository).findByAccountId(eq(account.getId()), any());

        // and: the rail account transaction are retrieved - using the permitted max history
        LocalDate startDate = LocalDate.now().minusDays(userConsent.getMaxHistory());
        verify(railAccountService).transactions(account.getRailAccountId(), startDate, LocalDate.now());

        // and: NO transactions are saved
        verify(accountTransactionRepository, never()).saveAll(any());

        // and: the account's last-polled date is updated and saved
        assertNotNull(account.getDateLastPolled());
        verify(accountRepository).save(account);
    }

    @Test
    public void testAccept_WithinGracePeriod() {
        // given: a grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        // and: a UserConsent, associated with the account to be processed, is still active
        UserConsent userConsent = mockUserConsent(ConsentStatus.GIVEN);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: an account that was last processed 30 minutes ago
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .dateLastPolled(Instant.now().minusSeconds(30 * 60))
            .userConsentId(userConsent.getId())
            .userId(userConsent.getUserId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
        when(accountRepository.findById(any())).thenReturn(Optional.of(account));

        // when: the fixture is called to process the account
        fixture.accept(account.getId());

        // then: the account is retrieved
        verify(accountRepository).findById(account.getId());

        // and: the user-consent is NOT retrieved
        verify(userConsentRepository, never()).findById(any());

        // and: NO rail account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balance record is saved
        verify(accountBalanceRepository, never()).save(any());

        // and: NO account's local transactions are searched - to determine date of last transaction
        verify(accountTransactionRepository, never()).findByAccountId(any(), any());

        // and: NO rail account transaction are retrieved - using the permitted max history
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verify(accountTransactionRepository, never()).saveAll(any());

        // and: the account's last-polled date is NOT updated
        assertNotNull(account.getDateLastPolled());
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void testAccept_UserConsentNoLongerActive() {
        // given: a grace period of 1 hour
        when(configuration.accountPollingInterval()).thenReturn(Duration.ofHours(1));

        // and: a UserConsent, associated with the account to be processed, is still active
        UserConsent userConsent = mockUserConsent(ConsentStatus.CANCELLED);
        when(userConsentRepository.findById(any())).thenReturn(Optional.of(userConsent));

        // and: an account that was last processed 2 hours ago
        Account account = Account.builder()
            .id(UUID.randomUUID())
            .dateLastPolled(Instant.now().minusSeconds(120 * 60))
            .userConsentId(userConsent.getId())
            .userId(userConsent.getUserId())
            .railAccountId(UUID.randomUUID().toString())
            .build();
        when(accountRepository.findById(any())).thenReturn(Optional.of(account));

        // when: the fixture is called to process the account
        fixture.accept(account.getId());

        // then: the account is retrieved
        verify(accountRepository).findById(account.getId());

        // and: the user-consent is retrieved
        verify(userConsentRepository).findById(account.getUserConsentId());

        // and: NO rail account balances are retrieved
        verify(railAccountService, never()).balances(any());

        // and: NO balance record is saved
        verify(accountBalanceRepository, never()).save(any());

        // and: NO account's local transactions are searched - to determine date of last transaction
        verify(accountTransactionRepository, never()).findByAccountId(any(), any());

        // and: NO rail account transaction are retrieved - using the permitted max history
        verify(railAccountService, never()).transactions(any(), any(), any());

        // and: NO transactions are saved
        verify(accountTransactionRepository, never()).saveAll(any());

        // and: the account's last-polled date is NOT updated
        assertNotNull(account.getDateLastPolled());
        verify(accountRepository, never()).save(any());
    }

    private UserConsent mockUserConsent(ConsentStatus status) {
        return UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID().toString())
            .requisitionId(UUID.randomUUID().toString())
            .agreementId(UUID.randomUUID().toString())
            .agreementExpires(Instant.now().plusSeconds(1000))
            .maxHistory(90)
            .status(status)
            .dateGiven(Instant.now().minusSeconds(2))
            .build();
    }

    private Balance mockBalance() {
        return Balance.builder()
            .balanceAmount(CurrencyAmount.builder().amount(nextFloat()).currency("GBP").build())
            .referenceDate(LocalDate.now().minusDays(nextInt()))
            .balanceType(randomAlphanumeric(5))
            .build();
    }

    private TransactionList mockTransactionList() {
        return TransactionList.builder()
            .booked(List.of(mockTransactionDetail(), mockTransactionDetail()))
            .pending(List.of(mockTransactionDetail(), mockTransactionDetail()))
            .build();
    }

    private TransactionDetail mockTransactionDetail() {
        return TransactionDetail.builder()
            .bookingDate(LocalDate.now().minusDays(nextInt()))
            .internalTransactionId(UUID.randomUUID().toString())
            .transactionId(UUID.randomUUID().toString())
            .transactionAmount(CurrencyAmount.builder().amount(nextFloat()).currency("GBP").build())
            .build();
    }
}
