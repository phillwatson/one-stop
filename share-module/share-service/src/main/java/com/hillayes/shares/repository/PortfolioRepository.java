package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.Portfolio;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class PortfolioRepository extends RepositoryBase<Portfolio, UUID> {
    public long deleteUsersPortfolios(UUID userId) {
        return delete("userId", userId);
    }

    /**
     * Returns the portfolios for the identified user in name order.
     *
     * @param userId the ID of the user to whom the portfolios belong.
     * @param pageIndex the (zero-based) index of the page to be returned
     * @param pageSize the size of the page.
     * @return the qualified sub-set of Portfolio records.
     */
    public Page<Portfolio> getUsersPortfolios(UUID userId, int pageIndex, int pageSize) {
        return pageAll("userId = :userId",
            pageIndex, pageSize,
            OrderBy.by("name"),
            Map.of("userId", userId));
    }
}
