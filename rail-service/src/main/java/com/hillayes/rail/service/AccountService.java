package com.hillayes.rail.service;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class AccountService {
    @Inject
    AccountRepository accountRepository;

    public Page<Account> getAccounts(UUID userId,
                                     int page,
                                     int pageSize) {
        log.info("Listing user's accounts [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<Account> result = accountRepository.findByUserId(userId, PageRequest.of(page, pageSize));

        log.debug("Listing account's transactions [accountId: {}, page: {}, pageSize: {}, size: {}]",
            userId, page, pageSize, result.getNumberOfElements());
        return result;
    }

    public List<Account> getAccountsByUserConsent(UserConsent userConsent) {
        log.info("Listing consent's accounts [consentId: {}]", userConsent.getId());
        return accountRepository.findByUserConsentId(userConsent.getId());
    }

    public Optional<Account> getAccount(UUID accountId) {
        log.info("Get user's accounts [accountId: {}]", accountId);
        return accountRepository.findById(accountId);
    }
}
