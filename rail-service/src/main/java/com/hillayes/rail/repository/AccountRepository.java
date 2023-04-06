package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    public Optional<Account> findByRailAccountId(String railAccountId);

    public Page<Account> findByUserId(UUID userId, Pageable pageable);

    public List<Account> findByUserConsentId(UUID consentId);
}
