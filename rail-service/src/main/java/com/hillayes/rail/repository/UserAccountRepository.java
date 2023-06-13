package com.hillayes.rail.repository;

import com.hillayes.rail.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.inject.Singleton;
import java.util.UUID;

@Singleton
public interface UserAccountRepository extends JpaRepository<Account, UUID> {
}
