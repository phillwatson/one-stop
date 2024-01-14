package com.hillayes.notification.repository;

import com.hillayes.commons.jpa.JpaRepositoryBase;
import com.hillayes.notification.domain.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class UserRepository extends JpaRepositoryBase<User, UUID> {
    public UserRepository() {
        super(User.class);
    }
}
