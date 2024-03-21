package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.UserConsent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserConsentRepository extends RepositoryBase<UserConsent, UUID> {
    public Page<UserConsent> findByUserId(UUID userId, int pageNumber, int pageSize) {
        return findByPage(find("userId", userId), pageNumber, pageSize);
    }

    public List<UserConsent> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Optional<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return findFirst("select u from UserConsent u where u.userId = :userId AND u.institutionId = :institutionId",
            Map.of("userId", userId, "institutionId", institutionId));
    }

    /**
     * Looks up a user consent by the reference assigned when the user's agreement was first
     * requested.
     *
     * @param reference the reference assigned when the user's agreement was first requested.
     * @return the user consent, if found.
     */
    public Optional<UserConsent> findByReference(String reference) {
        return find("reference", reference).firstResultOptional();
    }
}
