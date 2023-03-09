package com.hillayes.email.repository;

import com.hillayes.email.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public interface UserRepository extends JpaRepository<User, UUID> {
}
