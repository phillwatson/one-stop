package com.hillayes.rail.repository;

import com.hillayes.rail.domain.UserConsent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    public Page<UserConsent> findByUserId(UUID userId, Pageable pageable);

    public List<UserConsent> findByUserIdAndInstitutionId(UUID userId, String institutionId);
}
