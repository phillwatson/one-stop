package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedAdhocTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.*;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.TransactionFilter;
import com.hillayes.rail.service.UserConsentService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An adhoc task to retrieve the balance and transaction data for an identified
 * UserConsent and rail Account.
 */
@ApplicationScoped
@Slf4j
public class PollAccountAdhocTask extends AbstractNamedAdhocTask<PollAccountAdhocTask.Payload> {
    private final ServiceConfiguration configuration;
    private final UserConsentService userConsentService;
    private final ConsentEventSender consentEventSender;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final RailProviderFactory railProviderFactory;

    @RegisterForReflection
    public record Payload(
        UUID consentId,
        String railAccountId
    ) {}

    public PollAccountAdhocTask(ServiceConfiguration configuration,
                                UserConsentService userConsentService,
                                ConsentEventSender consentEventSender,
                                AccountRepository accountRepository,
                                AccountBalanceRepository accountBalanceRepository,
                                AccountTransactionRepository accountTransactionRepository,
                                RailProviderFactory railProviderFactory) {
        super("poll-account");
        this.configuration = configuration;
        this.userConsentService = userConsentService;
        this.consentEventSender = consentEventSender;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.railProviderFactory = railProviderFactory;
    }

    public String queueTask(UUID consentId, String railAccountId) {
        log.info("Queuing task [consentId: {}, railAccountId: {}]", consentId, railAccountId);
        return queueTask(new Payload(consentId, railAccountId));
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
     * task to repeat that when processing another account of the same consent.
     *
     * @param context the context containing the identifier of the Account to be updated.
     */
    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<Payload> context) {
        UUID consentId = context.getPayload().consentId();
        String railAccountId = context.getPayload().railAccountId();
        log.info("Processing Poll Account task [consentId: {}, railAccountId: {}]", consentId, railAccountId);

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
        RailAgreement railAgreement = railProviderApi.getAgreement(userConsent.getAgreementId()).orElse(null);
        if (railAgreement == null) {
            log.info("Unable to find rail-agreement [consentId: {}, railAgreementId: {}]",
                consentId, userConsent.getAgreementId());
            return TaskConclusion.COMPLETE;
        }

        if (railAgreement.getStatus() == AgreementStatus.SUSPENDED) {
            userConsentService.consentSuspended(userConsent.getId());
            return TaskConclusion.COMPLETE;
        }

        if (railAgreement.getStatus() == AgreementStatus.EXPIRED) {
            userConsentService.consentExpired(userConsent.getId());
            return TaskConclusion.COMPLETE;
        }

        RailAccount railAccount = railProviderApi.getAccount(railAgreement, railAccountId).orElse(null);
        if (railAccount == null) {
            log.info("Unable to find rail-account [consentId: {}, railAccountId: {}]", consentId, railAccountId);
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.getStatus() == RailAccountStatus.ERROR) {
            userConsentService.consentDenied(railProviderApi, ConsentResponse.builder()
                .consentReference(userConsent.getReference())
                .errorCode("RAIL-ERROR")
                .errorDescription("Account access denied")
                .build());
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.getStatus() == RailAccountStatus.SUSPENDED) {
            userConsentService.consentSuspended(userConsent.getId());
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.getStatus() == RailAccountStatus.EXPIRED) {
            userConsentService.consentExpired(userConsent.getId());
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.getStatus() != RailAccountStatus.READY) {
            log.debug("Retrying account as it's not ready [accountId: {}, railAccountId: {}, status: {}]",
                userConsent.getId(), railAccount.getId(), railAccount.getStatus());
            return TaskConclusion.INCOMPLETE;
        }

        Account account = getOrCreateAccount(userConsent, railAccount);

        // only process if not already polled within grace period
        Instant grace = Instant.now().minus(configuration.accountPollingInterval());
        if ((account.getDateLastPolled() != null) && (account.getDateLastPolled().isAfter(grace))) {
            log.debug("Skipping account polling [accountId: {}, lastPolled: {}]",
                account.getId(), account.getDateLastPolled());
            return TaskConclusion.COMPLETE;
        }

        log.debug("Polling account [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());
        updateBalances(account, railAccount);
        updateTransactions(railProviderApi, railAgreement, account);

        account.setDateLastPolled(Instant.now());
        accountRepository.save(account);

        return TaskConclusion.COMPLETE;
    }

    private Account getOrCreateAccount(UserConsent userConsent,
                                       RailAccount railAccount) {
        // retrieve existing account record by rail-id or IBAN
        return accountRepository.findByRailAccountId(railAccount.getId())
            .or(() -> accountRepository.findByIban(userConsent.getUserId(), railAccount.getIban()))
            .map(account -> {
                // this may be a new consent record for an expired/suspended consent
                account.setUserConsentId(userConsent.getId());

                // refresh account details
                account.setRailAccountId(railAccount.getId());
                account.setAccountName(railAccount.getName());
                account.setAccountType(railAccount.getAccountType());
                account.setIban(railAccount.getIban());
                account.setOwnerName(railAccount.getOwnerName());
                return account;
            })
            .orElseGet(() -> {
                Account account = accountRepository.save(Account.builder()
                    .userConsentId(userConsent.getId())
                        .userId(userConsent.getUserId())
                        .railAccountId(railAccount.getId())
                        .institutionId(userConsent.getInstitutionId())
                        .accountName(railAccount.getName())
                        .accountType(railAccount.getAccountType())
                        .iban(railAccount.getIban())
                        .ownerName(railAccount.getOwnerName())
                        .currency(railAccount.getCurrency())
                        .build());

                consentEventSender.sendAccountRegistered(userConsent, account);
                return account;
            });
    }

    private void updateBalances(Account account, RailAccount railAccount) {
        log.debug("Updating balances [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        // find most recent recorded balance for this account
        Instant lastReferenceDate = accountBalanceRepository.findMostRecentByAccountId(account.getId())
            .map(AccountBalance::getReferenceDate)
            .orElse(null);

        // if no balance found OR the last recorded balance is older than the current balance
        if ((lastReferenceDate == null) || (lastReferenceDate.isBefore(railAccount.getBalance().getDateTime()))) {
            accountBalanceRepository.save(AccountBalance.builder()
                .accountId(account.getId())
                .balanceType(railAccount.getBalance().getType())
                .referenceDate(railAccount.getBalance().getDateTime())
                .amount(railAccount.getBalance().getAmount())
                .build());
        }
    }

    private void updateTransactions(RailProviderApi railProviderApi, RailAgreement railAgreement, Account account) {
        log.debug("Updating transactions [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        LocalDate startDate;
        if (account.getId() != null) {
            // find the date of the most recent transaction we hold locally
            // we then use that as the start date for our rails request for transactions
            OrderBy sort = OrderBy.by("bookingDateTime").descending();
            TransactionFilter filter = TransactionFilter.builder()
                .userId(account.getUserId())
                .accountId(account.getId())
                .build();
            startDate = accountTransactionRepository.findByFilter(filter, 0, 1)
                .stream().findFirst()
                .map(transaction -> LocalDate.ofInstant(transaction.getBookingDateTime(), ZoneOffset.UTC)) // take date of most recent transaction
                .orElse(LocalDate.now().minusDays(railAgreement.getMaxHistory())); // or calculate date if no transactions found
            log.debug("Looking for transactions [accountId: {}, startDate: {}]", account.getId(), startDate);
        } else {
            startDate = LocalDate.now().minusDays(railAgreement.getMaxHistory());
        }

        // retrieve transactions from rail
        List<RailTransaction> details = railProviderApi.listTransactions(railAgreement, account.getRailAccountId(), startDate);

        // identify those internal transaction IDs we've seen before
        // TODO: For large lists, we should consider batching this to reduce memory load
        Set<String> existing = (account.getId() == null)
            ? Set.of()
            : accountTransactionRepository.findByInternalId(details.stream()
                .unordered()
                .map(RailTransaction::getId)
                .distinct()
                .toList())
            .stream()
            .map(AccountTransaction::getInternalTransactionId)
            .collect(Collectors.toUnmodifiableSet());

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
