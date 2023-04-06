package com.hillayes.rail.service;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountTransaction;
import com.hillayes.rail.repository.AccountRepository;
import com.hillayes.rail.repository.AccountTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class AccountService {
    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountTransactionRepository accountTransactionRepository;

    public Optional<Account> getAccount(UUID accountId) {
        log.info("Get user's accounts [accountId: {}]", accountId);
        return accountRepository.findById(accountId);
    }

    public Page<Account> getAccounts(UUID userId, int page, int pageSize) {
        log.info("Listing user's accounts [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);

        Pageable paging = PageRequest.of(page, pageSize);
        Page<Account> accounts = accountRepository.findByUserId(userId, paging);

        log.debug("Listing user's accounts [userId: {}, page: {}, pageSize: {}, count: {}]",
            userId, page, pageSize, accounts.getSize());
        return accounts;
    }

    public List<AccountTransaction> getTransactions(UUID accountId) {
        log.debug("Listing user's transactions [accountId: {}]", accountId);
        PageRequest byBookingDate = PageRequest.of(0, 100, Sort.by("bookingDate").descending());
        List<AccountTransaction> transactions = accountTransactionRepository.findByAccountId(accountId, byBookingDate);

        log.debug("Listing user's transactions [accountId: {}, size: {}]", accountId, transactions.size());
        return transactions;

    }
}
