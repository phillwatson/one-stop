package com.hillayes.rail.scheduled;

import com.hillayes.commons.Strings;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.*;
import com.hillayes.nordigen.model.AccountStatus;
import com.hillayes.nordigen.model.AccountSummary;
import com.hillayes.nordigen.model.TransactionDetail;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.UserConsentService;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
    private final RailAccountService railAccountService;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @RegisterForReflection
    public static class Payload {
        UUID consentId;
        String railAccountId;
    }

    public PollAccountJobbingTask(ServiceConfiguration configuration,
                                  UserConsentService userConsentService,
                                  AccountRepository accountRepository,
                                  AccountBalanceRepository accountBalanceRepository,
                                  AccountTransactionRepository accountTransactionRepository,
                                  RailAccountService railAccountService) {
        super("poll-account");
        this.configuration = configuration;
        this.userConsentService = userConsentService;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.railAccountService = railAccountService;
    }

    public String queueJob(UUID consentId, String railAccountId) {
        log.info("Queuing job [consentId: {}, railAccountId: {}]", consentId, railAccountId);
        Payload payload = Payload.builder()
            .consentId(consentId)
            .railAccountId(railAccountId)
            .build();
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
        UUID consentId = context.getPayload().consentId;
        String railAccountId = context.getPayload().railAccountId;
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

        AccountSummary railAccount = railAccountService.get(railAccountId).orElse(null);
        if (railAccount == null) {
            log.info("Unable to find rail-account [consentId: {}, railAccountId: {}]", consentId, railAccountId);
            return TaskConclusion.COMPLETE;
        }

        if (railAccount.status == AccountStatus.SUSPENDED) {
            userConsentService.consentSuspended(userConsent.getId());
        }

        else if (railAccount.status == AccountStatus.EXPIRED) {
            userConsentService.consentExpired(userConsent.getId());
        }

        else if (railAccount.status == AccountStatus.READY) {
            Account account = getOrCreateAccount(userConsent, railAccount);

            // only process if not already polled within grace period
            Instant grace = Instant.now().minus(configuration.accountPollingInterval());
            if ((account.getDateLastPolled() != null) && (account.getDateLastPolled().isAfter(grace))) {
                log.debug("Skipping account polling [accountId: {}, lastPolled: {}]",
                    account.getId(), account.getDateLastPolled());
                return TaskConclusion.COMPLETE;
            }

            log.debug("Polling account [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());
            updateBalances(account);
            updateTransactions(account, userConsent.getMaxHistory());

            account.setDateLastPolled(Instant.now());
            accountRepository.save(account);
        }

        return TaskConclusion.COMPLETE;
    }

    private Account getOrCreateAccount(UserConsent userConsent, AccountSummary railAccount) {
        return accountRepository.findByRailAccountId(railAccount.id)
            .map(account -> {
                // this may be a new consent record for an expired/suspended consent
                account.setUserConsentId(userConsent.getId());
                return account;
            })
            .orElseGet(() -> {
                Account newAccount = Account.builder()
                    .userConsentId(userConsent.getId())
                    .userId(userConsent.getUserId())
                    .institutionId(userConsent.getInstitutionId())
                    .railAccountId(railAccount.id)
                    .ownerName(railAccount.ownerName)
                    .iban(railAccount.iban)
                    .build();

                // retrieve rail-account details
                railAccountService.details(railAccount.id).ifPresent(details -> {
                    Map<String, Object> accountProperties = (Map) details.get("account");
                    if (accountProperties != null) {
                        newAccount.setAccountName((String) accountProperties.get("name"));
                        newAccount.setAccountType((String) accountProperties.get("cashAccountType"));
                        newAccount.setCurrencyCode((String) accountProperties.get("currency"));
                    }
                });

                return accountRepository.save(newAccount);
            });
    }

    private void updateBalances(Account account) {
        log.debug("Updating balances [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        List<AccountBalance> balances = railAccountService.balances(account.getRailAccountId())
            .orElse(List.of())
            .stream()
            .map(balance -> accountBalanceRepository.save(AccountBalance.builder()
                .accountId(account.getId())
                .balanceType(balance.balanceType)
                .referenceDate(balance.referenceDate)
                .currencyCode(balance.balanceAmount.currency)
                .amount(balance.balanceAmount.amount)
                .lastCommittedTransaction(balance.lastCommittedTransaction)
                .build()))
            .toList();

        log.debug("Updated balances [size: {}]", balances.size());
    }

    private void updateTransactions(Account account, int maxHistory) {
        log.debug("Updating transactions [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        LocalDate startDate;
        if (account.getId() != null) {
            // find the date of the most recent transaction we hold locally
            // we then use that as the start date for our rails request for transactions
            Sort sort = Sort.by("bookingDateTime").descending();
            startDate = accountTransactionRepository.findByAccountId(account.getId(), sort, 0, 1)
                .stream().findFirst()
                .map(transaction -> LocalDate.ofInstant(transaction.getBookingDateTime(), ZoneOffset.UTC)) // take date of most recent transaction
                .orElse(LocalDate.now().minusDays(maxHistory)); // or calculate date if no transactions found
            log.debug("Looking for transactions [accountId: {}, startDate: {}]", account.getId(), startDate);
        } else {
            startDate = LocalDate.now().minusDays(maxHistory);
        }

        // retrieve transactions from rail
        List<TransactionDetail> details = railAccountService.transactions(account.getRailAccountId(), startDate, LocalDate.now())
            .map(transactionList -> transactionList.booked)
            .orElse(List.of());

        // TODO: add CREATE INDEX idx_account_intrnl_id ON ${flyway:defaultSchema}.account_transaction (internal_transaction_id);
        // identify those internal transaction IDs we've seen before
        List<String> existing = accountTransactionRepository.findByInternalId(details.stream()
            .map(detail -> detail.internalTransactionId).toList())
            .stream().map(AccountTransaction::getInternalTransactionId).toList();

        // map the NEW transactions to our own records
        List<AccountTransaction> transactions = details.stream()
            .filter(detail -> !existing.contains(detail.internalTransactionId))
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
    private AccountTransaction marshalTransaction(Account account, TransactionDetail detail) {
        return AccountTransaction.builder()
            .userId(account.getUserId())
            .accountId(account.getId())
            .internalTransactionId(detail.internalTransactionId == null ? UUID.randomUUID().toString() : detail.internalTransactionId)
            .transactionId(detail.transactionId)
            .bookingDateTime(bestOf(detail.bookingDate, detail.bookingDateTime))
            .valueDateTime(bestOf(detail.valueDate, detail.valueDateTime))
            .transactionAmount((detail.transactionAmount == null) ? 0 : detail.transactionAmount.amount)
            .transactionCurrencyCode((detail.transactionAmount == null) ? null : detail.transactionAmount.currency)
            .additionalInformation(Strings.toStringOrNull(detail.additionalInformation))
            .additionalInformationStructured(Strings.toStringOrNull(detail.additionalInformationStructured))
            .balanceAfterTransaction(detail.balanceAfterTransaction)
            .bankTransactionCode(detail.bankTransactionCode)
            .checkId(detail.checkId)
            .creditorIban((detail.creditorAccount == null) ? null : detail.creditorAccount.iban)
            .creditorAgent(detail.creditorAgent)
            .creditorId(detail.creditorId)
            .creditorName(detail.creditorName)
            .currencyExchange(detail.currencyExchange)
            .debtorIban((detail.debtorAccount == null) ? null : detail.debtorAccount.iban)
            .debtorAgent(detail.debtorAgent)
            .debtorName(detail.debtorName)
            .endToEndId(detail.endToEndId)
            .entryReference(detail.entryReference)
            .mandateId(detail.mandateId)
            .merchantCategoryCode(detail.merchantCategoryCode)
            .proprietaryBankTransactionCode(detail.proprietaryBankTransactionCode)
            .purposeCode(detail.purposeCode)
            .remittanceInformationStructured(Strings.toStringOrNull(detail.remittanceInformationStructured))
            .remittanceInformationStructuredArray(Strings.toStringOrNull(detail.remittanceInformationStructuredArray))
            .remittanceInformationUnstructured(Strings.toStringOrNull(detail.remittanceInformationUnstructured))
            .remittanceInformationUnstructuredArray(Strings.toStringOrNull(detail.remittanceInformationUnstructuredArray))
            .ultimateCreditor(detail.ultimateCreditor)
            .ultimateDebtor(detail.ultimateDebtor)
            .build();
    }

    /**
     * Takes the best of the given date and instant; preferring the instant if both are present.
     * If neither are present, returns null.
     *
     * @param date the date to use if instant is null.
     * @param instant the instant to use if not null.
     * @return the best of the given date and instant.
     */
    private Instant bestOf(LocalDate date, Instant instant) {
        if (instant != null) {
            return instant;
        }
        return (date == null) ? instant : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
