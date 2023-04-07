package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    public Page<AccountTransaction> findByUserId(UUID userId, Pageable pageable);
    public Page<AccountTransaction> findByAccountId(UUID accountId, Pageable pageable);
}
