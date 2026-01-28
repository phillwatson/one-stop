package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HoldingRepository extends RepositoryBase<Holding, UUID> {
    /**
     * Returns the portfolio's holdings in share index name order.
     *
     * @param portfolio the portfolio whose holdings are to be returned.
     * @param pageIndex the (zero-based) index of the page to be returned.
     * @param pageSize the size of the page.
     * @return the qualified sub-set of Portfolio records.
     */
    public Page<Holding> getHolding(Portfolio portfolio, int pageIndex, int pageSize) {
        return pageAll("portfolio = :portfolio", pageIndex, pageSize,
            pageIndex, pageSize,
            OrderBy.by("shareIndex.name"),
            Map.of("portfolio", portfolio));
    }

    /**
     * Returns the holdings for the identified share within a portfolio
     * in ascending order of date created.
     *
     * @param portfolioId  the ID of the portfolio to which the holdings belong.
     * @param shareIndexId the ID of the share index to which the holdings relate.
     * @return the share holding record for the identified portfolio and share index.
     */
    public Optional<Holding> getHolding(UUID portfolioId, UUID shareIndexId) {
        return find("portfolioId = :portfolioId AND shareIndex.id = :shareIndexId",
            Map.of(
                "portfolioId", portfolioId,
                "shareIndexId", shareIndexId)
            ).firstResultOptional();
    }
}
