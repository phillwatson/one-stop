package com.hillayes.rail.service;

import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.AccountBalance;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.repository.AccountBalanceRepository;
import com.hillayes.rail.repository.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

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
    AccountBalanceRepository accountBalanceRepository;

    public Page<Account> getAccounts(UUID userId,
                                     int page,
                                     int pageSize) {
        log.info("Listing user's accounts [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<Account> result = accountRepository.findByUserId(userId, page, pageSize);

        log.debug("Listing account's transactions [accountId: {}, page: {}, pageSize: {}, size: {}]",
            userId, page, pageSize, result.getNumberOfElements());
        return result;
    }

    public List<Account> getAccountsByUserConsent(UserConsent userConsent) {
        log.info("Listing consent's accounts [consentId: {}]", userConsent.getId());
        return accountRepository.findByUserConsentId(userConsent.getId());
    }

    public Optional<Account> getAccount(UUID accountId) {
        log.info("Get user's account [accountId: {}]", accountId);
        return accountRepository.findByIdOptional(accountId);
    }

    /**
     * Returns the most recent balance records for the given Account. The result is
     * a list because, for a given date, multiple balances of different types can be
     * produced.
     *
     * @param account the account for which the balances are requested.
     * @return the list of balance records for the same, most recent date.
     */
    public List<AccountBalance> getMostRecentBalance(Account account) {
        log.info("Get user's account balance [accountId: {}]", account.getId());
        return accountBalanceRepository.findFirstByAccountIdOrderByReferenceDateDesc(account.getId())
            .map(balance -> accountBalanceRepository.findByAccountIdAndReferenceDate(account.getId(), balance.getReferenceDate()))
            .orElse(List.of());
    }
}
