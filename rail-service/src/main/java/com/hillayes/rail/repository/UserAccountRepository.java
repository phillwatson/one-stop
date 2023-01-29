package com.hillayes.rail.repository;

import com.hillayes.rail.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
}
