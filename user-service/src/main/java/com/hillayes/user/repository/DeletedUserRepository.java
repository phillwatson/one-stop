package com.hillayes.user.repository;

import com.hillayes.user.domain.DeletedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.inject.Singleton;
import java.util.UUID;

@Singleton
public interface DeletedUserRepository extends JpaRepository<DeletedUser, UUID> {
}
