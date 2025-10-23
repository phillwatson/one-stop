package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.DealingHistory;
import com.hillayes.shares.domain.Holding;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DealingHistoryRepository extends RepositoryBase<DealingHistory, UUID> {
    public Page<DealingHistory> getDealings(Holding holding, int pageNumber, int pageSize) {
        return getDealings(holding.getId(), pageNumber, pageSize);
    }

    public Page<DealingHistory> getDealings(UUID holdingId, int pageNumber, int pageSize) {
        return pageAll("shareHoldingId = :holdingId"
            , pageNumber, pageSize,
            OrderBy.by("marketDate"),
            Map.of("holdingId", holdingId));
    }
}
