package com.hillayes.user.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.user.domain.DeletedUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class DeletedUserRepository extends RepositoryBase<DeletedUser, UUID> {
}
