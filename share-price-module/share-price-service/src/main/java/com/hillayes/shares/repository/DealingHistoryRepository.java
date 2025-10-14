package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.DealingHistory;
import com.hillayes.shares.domain.ShareHolding;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class DealingHistoryRepository extends RepositoryBase<DealingHistory, UUID> {
    public Page<DealingHistory> getDealings(ShareHolding holding, int pageNumber, int pageSize) {
        return getDealings(holding.getId(), pageNumber, pageSize);
    }

    public Page<DealingHistory> getDealings(UUID holdingId, int pageNumber, int pageSize) {
        return findByPage(find("shareHoldingId = :holdingId",
            Sort.by("marketDate"),
            Parameters.with("holdingId", holdingId)), pageNumber, pageSize);
    }
}
