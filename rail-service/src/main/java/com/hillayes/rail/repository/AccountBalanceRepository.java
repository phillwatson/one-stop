package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    List<AccountBalance> findByAccountIdAndReferenceDate(UUID accountId, LocalDate referenceDate);

    Optional<AccountBalance> findFirstByAccountIdOrderByReferenceDateDesc(UUID accountId);
}
