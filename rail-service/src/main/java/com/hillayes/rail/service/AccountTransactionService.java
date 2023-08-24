package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class AccountTransactionService {
    @Inject
    AccountService accountService;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    /**
     * Returns a paged list of transactions for the identified user; optionally filtered by
     * the identified account. The transactions (and pages) are ordered by date, descending.
     *
     * @param userId the user to whom the transaction belong.
     * @param accountId the, optional, account to which the transaction belong.
     * @param page the, zero-based, page number of transactions.
     * @param pageSize the size of a page, and the maximum number of transactions to be returned.
     * @return the page of identified transactions.
     */
    public Page<AccountTransaction> getTransactions(UUID userId,
                                                    UUID accountId,
                                                    int page,
                                                    int pageSize) {
        log.info("Listing account's transactions [userId: {}, accountId: {}, page: {}, pageSize: {}]",
            userId, accountId, page, pageSize);

        verifyAccountHolder(userId, accountId);

        Sort sort = Sort.by("bookingDateTime").descending();
        Page<AccountTransaction> result = (accountId != null)
            ? accountTransactionRepository.findByAccountId(accountId, sort, page, pageSize)
            : accountTransactionRepository.findByUserId(userId, sort, page, pageSize);

        log.debug("Listing account's transactions [userId: {}, accountId: {}, page: {}, pageSize: {}, size: {}]",
            userId, accountId, page, pageSize, result.getContentSize());
        return result;
    }

    /**
     * Returns the transactions for the given user, and optionally filtered by account,
     * over the given date range; ordered by booking-datetime, earliest first.
     *
     * @param userId the user to whom the transaction belong.
     * @param accountId the account to which the transaction belong (optional).
     * @param fromDate the inclusive start date of the search.
     * @param toDate the inclusive end date of the search.
     * @return the list of transaction, ordered by booking-datetime.
     */
    public List<AccountTransaction> getTransactions(UUID userId,
                                                    UUID accountId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate) {
        log.info("Listing transaction [userId: {}, accountId: {}, from: {}, to: {}]",
            userId, accountId, fromDate, toDate);

        verifyAccountHolder(userId, accountId);

        // convert dates to instant
        Instant from = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AccountTransaction> result = (accountId != null)
            ? accountTransactionRepository.findByAccountAndDateRange(accountId, from, to)
            : accountTransactionRepository.findByUserAndDateRange(userId, from, to);

        log.info("Listing transaction [userId: {}, accountId: {}, from: {}, to: {}, size: {}]",
            userId, accountId, fromDate, toDate, result.size());
        return result;
    }

    public Optional<AccountTransaction> getTransaction(UUID transactionId) {
        log.info("Get transactions [transactionId: {}]", transactionId);
        return accountTransactionRepository.findByIdOptional(transactionId);
    }

    /**
     * Verifies that the identified user owns the identified account. If the account
     * ID is null, no verification is performed.
     * @param userId the user attempting to access the account.
     * @param accountId the optional account identifier.
     */
    private void verifyAccountHolder(UUID userId, UUID accountId) {
        if (accountId != null) {
            // verify that the account belongs to the user
            accountService.getAccount(accountId)
                .filter(account -> userId.equals(account.getUserId()) )
                .orElseThrow(() -> new NotFoundException("Account", accountId));
        }
    }
}
