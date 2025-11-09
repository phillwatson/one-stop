package com.hillayes.shares.repository;

import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareIndex;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShareIndexRepository extends RepositoryBase<ShareIndex, UUID> {
    public Optional<ShareIndex> findByIdentity(ShareIndex.ShareIdentity identity) {
        StringBuilder query = new StringBuilder();
        Map<String,Object> params = new HashMap<>(2);
        if (Strings.isNotBlank(identity.getIsin())) {
            query.append("identity.isin = :isin");
            params.put("isin", identity.getIsin());
        }
        if (Strings.isNotBlank(identity.getTickerSymbol())) {
            if (!query.isEmpty()) query.append(" AND ");
            query.append("identity.tickerSymbol = :tickerSymbol");
            params.put("tickerSymbol", identity.getTickerSymbol());
        }

        return findFirst(query.toString(), params);
    }

    /**
     * Returns a page of ShareIndex records in name order.
     *
     * @param pageNumber the (zero-based) page index.
     * @param pageSize the size of the page.
     * @return the qualified sub-set of ShareIndex records.
     */
    public Page<ShareIndex> listAll(int pageNumber, int pageSize) {
        return pageAll(OrderBy.by("name"), pageNumber, pageSize);
    }
}
