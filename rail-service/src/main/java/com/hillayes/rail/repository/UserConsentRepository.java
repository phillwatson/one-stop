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
        return pageAll("userId", pageNumber, pageSize, userId);
    }

    public List<UserConsent> findByUserId(UUID userId) {
        return listAll("userId", userId);
    }

    public List<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return listAll("userId = :userId AND institutionId = :institutionId",
            Map.of("userId", userId, "institutionId", institutionId));
    }
}
