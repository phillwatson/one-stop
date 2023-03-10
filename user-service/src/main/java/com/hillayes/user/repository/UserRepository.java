package com.hillayes.user.repository;

import com.hillayes.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public interface UserRepository extends JpaRepository<User, UUID> {
    public Optional<User> findByUsername(String username);
    public List<User> findByEmail(String email);
}
