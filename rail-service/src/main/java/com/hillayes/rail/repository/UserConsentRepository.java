package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.UserConsent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserConsentRepository extends RepositoryBase<UserConsent, UUID> {
    public Page<UserConsent> findByUserId(UUID userId, int pageNumber, int pageSize) {
        return findByPage(find("userId", userId), pageNumber, pageSize);
    }

    public List<UserConsent> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public List<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return listAll("select u from UserConsent u where u.userId = :userId AND u.institutionId = :institutionId",
            Map.of("userId", userId, "institutionId", institutionId));
    }
}
