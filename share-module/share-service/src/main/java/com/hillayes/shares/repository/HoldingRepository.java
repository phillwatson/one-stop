package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.Holding;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HoldingRepository extends RepositoryBase<Holding, UUID> {
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
