package com.hillayes.rail.service;

import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class AccountTransactionService {
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

        PageRequest byBookingDate = PageRequest.of(page, pageSize, Sort.by("bookingDateTime").descending());
        Page<AccountTransaction> result = (accountId != null)
            ? accountTransactionRepository.findByAccountId(accountId, byBookingDate)
            : accountTransactionRepository.findByUserId(userId, byBookingDate);

        log.debug("Listing account's transactions [userId: {}, accountId: {}, page: {}, pageSize: {}, size: {}]",
            userId, accountId, page, pageSize, result.getNumberOfElements());
        return result;
    }

    public Optional<AccountTransaction> getTransaction(UUID transactionId) {
        log.info("Get transactions [transactionId: {}]", transactionId);
        return accountTransactionRepository.findById(transactionId);
    }
}
