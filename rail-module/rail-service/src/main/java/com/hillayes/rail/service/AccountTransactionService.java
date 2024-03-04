package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import com.hillayes.rail.repository.TransactionFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

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
     * Returns the transactions for the given user, and optionally filtered by the
     * given properties; ordered by booking-datetime, descending.
     *
     * @param userId the user to whom the transaction belong.
     * @param filter the filter to apply to the transaction properties.
     * @param page the, zero-based, page number of transactions.
     * @param pageSize the size of a page, and the maximum number of transactions to be returned.
     * @return the page of identified transactions.
     */
    public Page<AccountTransaction> getTransactions(UUID userId,
                                                    TransactionFilter filter,
                                                    int page,
                                                    int pageSize) {
        log.info("Listing transaction [userId: {}, filter: {}]", userId, filter);

        if (filter == null) {
            filter = TransactionFilter.NULL;
        } else {
            verifyAccountHolder(userId, filter.getAccountId());
        }

        Page<AccountTransaction> result = accountTransactionRepository.findByFilter(userId, filter, page, pageSize);

        log.info("Listing transaction [userId: {}, filter: {}, size: {}, total: {}]",
            userId, filter, result.getContentSize(), result.getTotalCount());
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
            accountService.getAccount(userId, accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId));
        }
    }

}
