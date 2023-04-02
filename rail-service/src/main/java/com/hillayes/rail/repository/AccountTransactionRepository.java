package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
}
