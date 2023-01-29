package com.hillayes.rail.repository;

import com.hillayes.rail.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    public List<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId);
}
