package com.hillayes.rail.repository;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.Account;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountRepository extends RepositoryBase<Account, UUID> {
    public Optional<Account> findByRailAccountId(String railAccountId) {
        return findFirst("railAccountId", railAccountId);
    }

    /**
     * Returns the first Account with the given IBAN. If no account is found, or
     * the given IBAN is null or an empty string, the return value will be empty.
     * The userId is used to ensure no cross-account conflicts, should the IBAN
     * not be unique (for some unknown reason).
     *
     * @param userId the ID of the user for whom the account is being retrieved.
     * @param iban the IBAN of the account.
     * @return the identified account, or an empty result.
     */
    public Optional<Account> findByIban(UUID userId, String iban) {
        return (Strings.isBlank(iban))
            ? Optional.empty()
            : findFirst("userId = :userId and iban = :iban",
                Map.of("userId", userId, "iban", iban));
    }

    public Page<Account> findByUserId(UUID userId, int pageNumber, int pageSize) {
        return pageAll("userId", pageNumber, pageSize, userId);
    }

    public List<Account> findByUserConsentId(UUID consentId) {
        return listAll("userConsentId", consentId);
    }
}
