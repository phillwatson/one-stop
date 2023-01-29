package com.hillayes.user.repository;

import com.hillayes.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public interface UserRepository extends JpaRepository<User, UUID> {
}
