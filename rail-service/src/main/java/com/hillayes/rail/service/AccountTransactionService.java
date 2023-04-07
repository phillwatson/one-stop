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
