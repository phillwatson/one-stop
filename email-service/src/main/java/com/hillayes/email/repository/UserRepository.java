package com.hillayes.email.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.email.domain.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UserRepository extends RepositoryBase<User, UUID> {
}
