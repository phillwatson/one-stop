package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareIndex;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShareIndexRepository extends RepositoryBase<ShareIndex, UUID> {
    public Optional<ShareIndex> findByIsin(String isin) {
        return findFirst("isin", isin);
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
