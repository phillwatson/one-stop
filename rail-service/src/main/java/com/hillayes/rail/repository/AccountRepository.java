package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.Account;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountRepository extends RepositoryBase<Account, UUID> {
    public Optional<Account> findByRailAccountId(String railAccountId) {
        return find("railAccountId", railAccountId)
            .firstResultOptional();
    }

    public Page<Account> findByUserId(UUID userId, int pageNumber, int pageSize) {
        return findByPage(find("userId", userId), pageNumber, pageSize);
    }

    public List<Account> findByUserConsentId(UUID consentId) {
        return find("userConsentId", consentId)
            .list();
    }
}
