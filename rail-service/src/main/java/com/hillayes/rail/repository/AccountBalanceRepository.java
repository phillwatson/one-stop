package com.hillayes.rail.repository;

import com.hillayes.rail.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
}
