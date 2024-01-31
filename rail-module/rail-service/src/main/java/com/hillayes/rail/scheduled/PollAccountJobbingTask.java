package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.AccountStatus;
import com.hillayes.rail.api.domain.RailAccount;
import com.hillayes.rail.api.domain.RailTransaction;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.service.UserConsentService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * A jobbing task to retrieve the balance and transaction data for an identified
 * UserConsent and rail Account.
 */
@ApplicationScoped
@Slf4j
public class PollAccountJobbingTask extends AbstractNamedJobbingTask<PollAccountJobbingTask.Payload> {
    private final ServiceConfiguration configuration;
    private final UserConsentService userConsentService;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final RailProviderFactory railProviderFactory;

    @RegisterForReflection
    public record Payload(
        UUID consentId,
        String railAccountId
    ) {}

    public PollAccountJobbingTask(ServiceConfiguration configuration,
                                  UserConsentService userConsentService,
                                  AccountRepository accountRepository,
                                  AccountBalanceRepository accountBalanceRepository,
                                  AccountTransactionRepository accountTransactionRepository,
                                  RailProviderFactory railProviderFactory) {
        super("poll-account");
        this.configuration = configuration;
        this.userConsentService = userConsentService;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.railProviderFactory = railProviderFactory;
    }

    public String queueJob(UUID consentId, String railAccountId) {
        log.info("Queuing job [consentId: {}, railAccountId: {}]", consentId, railAccountId);
        Payload payload = new Payload(consentId, railAccountId);
        return scheduler.addJob(this, payload);
    }

    /**
     * Performs the task of updating the balance and transaction date for the
     * identified Account. If the account data has already been updated within
     * the grace period (defined in the configuration account-polling-interval)
     * then no update is performed.
     *
     * This will obtain a lock on the identified consent record. One consent may
     * refer to several accounts, all of which may be being processed at the same
     * time. If we need to suspend or expire the consent, we don't want another
     * job to repeat that when processing another account of the same consent.
     *
     * @param context the context containing the identifier of the Account to be updated.
     */
    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<Payload> context) {
        UUID consentId = context.getPayload().consentId();
        String railAccountId = context.getPayload().railAccountId();
        log.info("Processing Poll Account job [consentId: {}, railAccountId: {}]", consentId, railAccountId);

        // get a lock on the consent to ensure no other updates
        UserConsent userConsent = userConsentService.lockUserConsent(consentId).orElse(null);
        if (userConsent == null) {
            log.info("Unable to find user-consent [consentId: {}, railAccountId: {}]", consentId, railAccountId);
            return TaskConclusion.COMPLETE;
        }

        if (userConsent.getStatus() != ConsentStatus.GIVEN) {
            log.debug("Skipping account polling [consentId: {}, railAccountId: {}, consentStatus: {}]",
                userConsent.getId(), railAccountId, userConsent.getStatus());
            return TaskConclusion.COMPLETE;
        }

        RailProviderApi railProviderApi = railProviderFactory.get(userConsent.getProvider());
        RailAccount railAccount = railProviderApi.getAccount(railAccountId).orElse(null);
        if (railAccount == null) {
            log.info("Unable to find rail-account [consentId: {}, railAccountId: {}]", consentId, railAccountId);
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.getStatus() == AccountStatus.SUSPENDED) {
            userConsentService.consentSuspended(userConsent.getId());
        }

        else if (railAccount.getStatus() == AccountStatus.EXPIRED) {
            userConsentService.consentExpired(userConsent.getId());
        }

        else if (railAccount.getStatus() == AccountStatus.READY) {
            com.hillayes.rail.domain.Account account = getOrCreateAccount(userConsent, railAccount);

            // only process if not already polled within grace period
            Instant grace = Instant.now().minus(configuration.accountPollingInterval());
            if ((account.getDateLastPolled() != null) && (account.getDateLastPolled().isAfter(grace))) {
                log.debug("Skipping account polling [accountId: {}, lastPolled: {}]",
                    account.getId(), account.getDateLastPolled());
                return TaskConclusion.COMPLETE;
            }

            log.debug("Polling account [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());
            updateBalances(railProviderApi, account);
            updateTransactions(railProviderApi, account, userConsent.getMaxHistory());

            account.setDateLastPolled(Instant.now());
            accountRepository.save(account);
        }

        return TaskConclusion.COMPLETE;
    }

    private Account getOrCreateAccount(UserConsent userConsent,
                                       RailAccount railAccount) {
        return accountRepository.findByRailAccountId(railAccount.getId())
            .map(account -> {
                // this may be a new consent record for an expired/suspended consent
                account.setUserConsentId(userConsent.getId());
                return account;
            })
            .orElseGet(() -> accountRepository.save(Account.builder()
                .userConsentId(userConsent.getId())
                .userId(userConsent.getUserId())
                .railAccountId(railAccount.getId())
                .institutionId(userConsent.getInstitutionId())
                .accountName(railAccount.getName())
                .iban(railAccount.getIban())
                .ownerName(railAccount.getOwnerName())
                .currency(railAccount.getCurrency())
                .accountType(railAccount.getAccountType())
                .build())
            );
    }

    private void updateBalances(RailProviderApi railProviderApi, Account account) {
        log.debug("Updating balances [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        List<AccountBalance> balances = railProviderApi.listBalances(account.getRailAccountId(), LocalDate.now().minusDays(90))
            .stream()
            .map(balance -> accountBalanceRepository.save(AccountBalance.builder()
                .accountId(account.getId())
                .balanceType(balance.getType())
                .referenceDate(balance.getDateTime())
                .amount(balance.getAmount())
                .build()))
            .toList();

        log.debug("Updated balances [size: {}]", balances.size());
    }

    private void updateTransactions(RailProviderApi railProviderApi, Account account, int maxHistory) {
        log.debug("Updating transactions [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        LocalDate startDate;
        if (account.getId() != null) {
            // find the date of the most recent transaction we hold locally
            // we then use that as the start date for our rails request for transactions
            OrderBy sort = OrderBy.by("bookingDateTime").descending();
            startDate = accountTransactionRepository.findByAccountId(account.getId(), sort, 0, 1)
                .stream().findFirst()
                .map(transaction -> LocalDate.ofInstant(transaction.getBookingDateTime(), ZoneOffset.UTC)) // take date of most recent transaction
                .orElse(LocalDate.now().minusDays(maxHistory)); // or calculate date if no transactions found
            log.debug("Looking for transactions [accountId: {}, startDate: {}]", account.getId(), startDate);
        } else {
            startDate = LocalDate.now().minusDays(maxHistory);
        }

        // retrieve transactions from rail
        List<RailTransaction> details = railProviderApi.listTransactions(account.getRailAccountId(), startDate, LocalDate.now());

        // identify those internal transaction IDs we've seen before
        List<String> existing = (account.getId() == null)
            ? List.of()
            : accountTransactionRepository.findByInternalId(details.stream()
                .map(RailTransaction::getId).toList()).stream()
            .map(AccountTransaction::getInternalTransactionId).toList();

        // map the NEW transactions to our own records
        List<AccountTransaction> transactions = details.stream()
            .filter(detail -> !existing.contains(detail.getId()))
            .map(detail -> marshalTransaction(account, detail))
            .toList();

        if (transactions.isEmpty()) {
            log.debug("No transactions found [accountId: {}, startDate: {}]", account.getId(), startDate);
        } else {
            log.debug("Persisting transactions [size: {}]", transactions.size());
            accountTransactionRepository.saveAll(transactions);
        }
    }

    /**
     * Builds an AccountTransaction record from the given rail transaction.
     *
     * @param account the internal account to which the transaction belongs.
     * @param detail the rail transaction details.
     * @return the AccountTransaction created from the given rail transaction.
     */
    private AccountTransaction marshalTransaction(Account account, RailTransaction detail) {
        return AccountTransaction.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .internalTransactionId(detail.getId())
            .transactionId(detail.getOriginalTransactionId())
            .bookingDateTime(detail.getDateBooked())
            .valueDateTime(detail.getDateValued())
            .amount(detail.getAmount())
            .additionalInformation(detail.getDescription())
            .creditorName(detail.getCreditor())
            .reference(detail.getReference())
            .build();
    }
}
