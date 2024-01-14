package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.JpaRepositoryBase;
import com.hillayes.commons.jpa.Page;
import com.hillayes.rail.domain.UserConsent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserConsentRepository extends JpaRepositoryBase<UserConsent, UUID> {
    public UserConsentRepository() {
        super(UserConsent.class);
    }

    public Page<UserConsent> findByUserId(UUID userId, int pageNumber, int pageSize) {
        return pageAll("select c from UserConsent c where c.userId=?1", pageNumber, pageSize, userId);
    }

    public List<UserConsent> findByUserId(UUID userId) {
        return listAll("select u from UserConsent u where u.userId=?1", userId);
    }

    public List<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return listAll("select u from UserConsent u where u.userId = :userId AND u.institutionId = :institutionId",
            Map.of("userId", userId, "institutionId", institutionId));
    }
}
