package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareHolding;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ShareHoldingRepository extends RepositoryBase<ShareHolding, UUID> {
    public long deleteUsersHoldings(UUID userId) {
        return delete("userId", userId);
    }

    public Page<ShareHolding> getUsersHoldings(UUID userId, int pageNumber, int pageSize) {
        return findByPage(find("userId = :userId",
            Sort.by("shareIndexId"),
            Parameters.with("userId", userId)), pageNumber, pageSize);
    }
}
