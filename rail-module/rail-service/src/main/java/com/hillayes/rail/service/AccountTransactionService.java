package com.hillayes.rail.service;

import com.hillayes.commons.MonetaryAmount;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.TransactionFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
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
    CategoryService categoryService;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    public Optional<AccountTransaction> getTransaction(UUID transactionId) {
        log.info("Get transactions [transactionId: {}]", transactionId);
        return accountTransactionRepository.findByIdOptional(transactionId);
    }

    /**
     * Returns the transactions for the given user, and optionally filtered by the
     * given properties; ordered by booking-datetime, descending.
     *
     * @param filter the filter to apply to the transaction properties.
     * @param page the, zero-based, page number of transactions.
     * @param pageSize the size of a page, and the maximum number of transactions to be returned.
     * @return the page of identified transactions.
     */
    public Page<AccountTransaction> getTransactions(TransactionFilter filter,
                                                    int page,
                                                    int pageSize) {
        log.info("Listing transaction [filter: {}]", filter);

        if (filter == null) {
            filter = TransactionFilter.NULL;
        } else {
            verifyAccountHolder(filter.getUserId(), filter.getAccountId());
        }

        Page<AccountTransaction> result = accountTransactionRepository.findByFilter(filter, page, pageSize);

        log.info("Listing transaction [filter: {}, size: {}, total: {}]",
            filter, result.getContentSize(), result.getTotalCount());
        return result;
    }

    public List<MonetaryAmount> getTransactionTotals(TransactionFilter filter) {
        log.info("Get transaction totals [filter: {}]", filter);

        if (filter == null) {
            filter = TransactionFilter.NULL;
        } else {
            verifyAccountHolder(filter.getUserId(), filter.getAccountId());
        }

        return accountTransactionRepository.findTotals(filter);
    }

    public List<AccountTransaction> findByCategory(UUID userId, UUID groupId, UUID categoryId,
                                                   Instant startDate, Instant endDate) {
        log.info("Get transactions by category [userId: {}, categoryId: {}, startDate: {}, endDate: {}]",
            userId, categoryId, startDate, endDate);

        List<AccountTransaction> result;
        if (categoryId == null) {
            result = accountTransactionRepository.findUncategorised(userId, groupId, startDate, endDate);
        } else {
            // ensure the category belongs to the user
            categoryService.getCategory(userId, categoryId);

            result = accountTransactionRepository.findByCategory(userId, categoryId, startDate, endDate);
        }

        log.info("Get transactions by category [userId: {}, categoryId: {}, startDate: {}, endDate: {}, total: {}]",
            userId, categoryId, startDate, endDate, result.size());
        return result;
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
            accountService.getAccount(userId, accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId));
        }
    }
}
