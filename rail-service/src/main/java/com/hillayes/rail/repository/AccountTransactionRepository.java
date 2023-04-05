package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    public List<AccountTransaction> findByAccountId(UUID accountId, Pageable pageable);
}
