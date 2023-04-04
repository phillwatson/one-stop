package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    @Query(nativeQuery = true, value = "SELECT t FROM account_transaction t WHERE t.account_id = ?1 ORDER BY t.booking_date DESC LIMIT 1")
    public Optional<AccountTransaction> getMostRecent(UUID accountId);
}
