package com.hillayes.ftmarket.api.repository;

import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class IsinIssueLookupRepository extends RepositoryBase<IsinIssueLookup, String> {
    /**
     * Returns the IsinIssueLookup entry with the given ISIN. ISIN values are unique.
     *
     * @param isin the ISIN to search for.
     * @return the IsinIssueLookup record found with the given ISIN, if any.
     */
    public Optional<IsinIssueLookup> findByIsin(String isin) {
        return findFirst("isin", isin);
    }
}
