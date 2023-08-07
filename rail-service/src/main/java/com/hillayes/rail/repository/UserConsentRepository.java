package com.hillayes.rail.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.rail.domain.UserConsent;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.data.domain.Page;

import java.util.List;
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
        return find("userId = :userId AND institutionId = :institutionId",
            Parameters.with("userId", userId).and("institutionId", institutionId))
            .list();
    }
}
