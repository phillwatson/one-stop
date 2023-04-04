package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.tasks.NamedJobbingTask;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionsResponse;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.service.RailAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A jobbing task to retrieve the balance and transaction data for an identified
 * Account.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PollAccountSchedulerTask implements NamedJobbingTask<UUID> {
    private final ServiceConfiguration configuration;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final RailAccountService railAccountService;

    private SchedulerFactory scheduler;

    @Override
    public String getName() {
        return "poll-account";
    }

    @Override
    public void taskScheduled(SchedulerFactory scheduler) {
        log.info("PollAccountTask.taskScheduled()");
        this.scheduler = scheduler;
    }

    @Override
    public String queueJob(UUID accountId) {
        log.info("Queuing PollAccountTask job [accountId: {}]", accountId);
        return scheduler.addJob(this, accountId);
    }

    /**
     * Performs the task of updating the balance and transaction date for the
     * identified Account. If the account data has already been updated within
     * the grace period (defined in the configuration account-polling-interval)
     * then no update is performed.
     *
     * @param accountId the identifier of the Account to be updated.
     */
    @Override
    @Transactional
    public void accept(UUID accountId) {
        log.info("Processing PollAccountTask job [accountId: {}]", accountId);

        // if the account has been polled after this grace period, don't poll it again
        Instant grace = Instant.now().minus(configuration.accountPollingInterval());

        accountRepository.findById(accountId)
            .filter(account -> {
                if ((account.getDateLastPolled() == null) || (account.getDateLastPolled().isBefore(grace))) {
                    return true;
                }

                log.debug("Skipping account transaction update [accountId: {}, lastPolled: {}]",
                    accountId, account.getDateLastPolled());
                return false;
            })
            .ifPresent(account -> {
                log.debug("Polling account [accountId: {}, railAccountId: {}]", accountId, account.getRailAccountId());
                updateBalances(account);
                updateTransactions(account);

                account.setDateLastPolled(Instant.now());
                accountRepository.save(account);
            });
    }

    private void updateBalances(Account account) {
        log.debug("Updating balances [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        AccountBalanceList balanceList = railAccountService.balances(account.getRailAccountId());
        if ((balanceList != null) && (balanceList.balances != null)) {
            List<AccountBalance> balances = balanceList.balances.stream()
                .map(balance ->
                    accountBalanceRepository.save(AccountBalance.builder()
                        .accountId(account.getId())
                        .balanceType(balance.balanceType)
                        .referenceDate(balance.referenceDate)
                        .currency(balance.balanceAmount.currency)
                        .amount(balance.balanceAmount.amount)
                        .build())
                )
                .toList();
            log.debug("Updated balances [size: {}]", balances.size());
        }
    }

    private void updateTransactions(Account account) {
        log.debug("Updating transactions [accountId: {}, railAccountId: {}]", account.getId(), account.getRailAccountId());

        LocalDate startDate = accountTransactionRepository.getMostRecent(account.getId())
            .map(AccountTransaction::getBookingDate)
            .orElse(LocalDate.of(2020, 1, 1));
        log.debug("Looking for transactions [accountId: {}, startDate: {}]", account.getId(), startDate);

        TransactionsResponse transactionResponse = railAccountService.transactions(account.getRailAccountId(), startDate, LocalDate.now());
        if ((transactionResponse == null) || (transactionResponse.transactions == null) || (transactionResponse.transactions.booked == null)) {
            log.debug("No transaction found for dates");
            return;
        }

        List<AccountTransaction> transactions = transactionResponse.transactions.booked.stream()
            .map(detail -> AccountTransaction.builder()
                .accountId(account.getId())
                .internalTransactionId(detail.internalTransactionId)
                .transactionId(detail.transactionId)
                .bookingDate(detail.bookingDate)
                .bookingDateTime(detail.bookingDateTime)
                .valueDate(detail.valueDate)
                .valueDateTime(detail.valueDateTime)
                .transactionAmount((detail.transactionAmount == null) ? null : detail.transactionAmount.amount)
                .transactionCurrency((detail.transactionAmount == null) ? null : detail.transactionAmount.currency)
                .additionalInformation(detail.additionalInformation)
                .additionalInformationStructured(detail.additionalInformationStructured)
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
                .remittanceInformationStructured(detail.remittanceInformationStructured)
                .remittanceInformationStructuredArray(detail.remittanceInformationStructuredArray)
                .remittanceInformationUnstructured(detail.remittanceInformationUnstructured)
                .remittanceInformationUnstructuredArray(detail.remittanceInformationUnstructuredArray)
                .ultimateCreditor(detail.ultimateCreditor)
                .ultimateDebtor(detail.ultimateDebtor)
                .build())
            .toList();

        accountTransactionRepository.saveAll(transactions);
        log.debug("Updated transactions [size: {}]", transactions.size());
    }
}
