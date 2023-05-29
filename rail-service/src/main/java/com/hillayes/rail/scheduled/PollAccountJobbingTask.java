package com.hillayes.rail.scheduled;

import com.hillayes.commons.Strings;
import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.model.TransactionDetail;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.service.RailAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * A jobbing task to retrieve the balance and transaction data for an identified
 * Account.
 */
@ApplicationScoped
@Slf4j
public class PollAccountJobbingTask extends AbstractNamedJobbingTask<UUID> {
    private final ServiceConfiguration configuration;
    private final UserConsentRepository userConsentRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final RailAccountService railAccountService;

    public PollAccountJobbingTask(ServiceConfiguration configuration,
                                  UserConsentRepository userConsentRepository,
                                  AccountRepository accountRepository,
                                  AccountBalanceRepository accountBalanceRepository,
                                  AccountTransactionRepository accountTransactionRepository,
                                  RailAccountService railAccountService) {
        super("poll-account");
        this.configuration = configuration;
        this.userConsentRepository = userConsentRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.railAccountService = railAccountService;
    }

    /**
     * Performs the task of updating the balance and transaction date for the
     * identified Account. If the account data has already been updated within
     * the grace period (defined in the configuration account-polling-interval)
     * then no update is performed.
     *
     * @param context the context containing the identifier of the Account to be updated.
     */
    @Override
    @Transactional
    public TaskConclusion apply(TaskContext<UUID> context) {
        UUID accountId = context.getPayload();
        log.info("Processing Poll Account job [accountId: {}]", accountId);

        // if the account has been polled after this grace period, don't poll it again
        Instant grace = Instant.now().minus(configuration.accountPollingInterval());

        accountRepository.findById(accountId)
            .filter(acc -> {
                // only process if not polled within grace period
                if ((acc.getDateLastPolled() == null) || (acc.getDateLastPolled().isBefore(grace))) {
                    return true;
                }

                log.debug("Skipping account transaction update [accountId: {}, lastPolled: {}]",
                    accountId, acc.getDateLastPolled());
                return false;
            })
            .ifPresent(account -> userConsentRepository.findById(account.getUserConsentId())
                // ensure consent is still active
                .filter(consent -> {
                    if (consent.getStatus() == ConsentStatus.GIVEN) {
                        return true;
                    }

                    log.debug("Skipping account transaction update [accountId: {}, consentId: {}, status: {}]",
                        accountId, consent.getId(), consent.getStatus());
                    return false;
                })
                .ifPresent(consent -> {
                    log.debug("Polling account [accountId: {}, railAccountId: {}]", accountId, account.getRailAccountId());
                    updateBalances(account);
                    updateTransactions(account, consent.getMaxHistory());

                    account.setDateLastPolled(Instant.now());
                    accountRepository.save(account);
                })
            );

        return TaskConclusion.COMPLETE;
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
                .build()))
            .toList();

        log.debug("Updated balances [size: {}]", balances.size());
    }

    private void updateTransactions(Account account, int maxHistory) {
        log.debug("Updating transactions [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        PageRequest byBookingDate = PageRequest.of(0, 1, Sort.by("bookingDateTime").descending());
        LocalDate startDate = accountTransactionRepository.findByAccountId(account.getId(), byBookingDate)
            .stream().findFirst()
            .map(transaction -> LocalDate.ofInstant(transaction.getBookingDateTime(), ZoneOffset.UTC)) // take date of most recent transaction
            .orElse(LocalDate.now().minusDays(maxHistory)); // or calculate date if no transactions found
        log.debug("Looking for transactions [accountId: {}, startDate: {}]", account.getId(), startDate);

        List<AccountTransaction> transactions = railAccountService.transactions(account.getRailAccountId(), startDate, LocalDate.now())
            .map(transactionList -> transactionList.booked)
            .orElse(List.of())
            .stream()
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
