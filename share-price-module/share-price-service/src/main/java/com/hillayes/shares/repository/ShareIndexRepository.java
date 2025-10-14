package com.hillayes.shares.repository;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.jpa.RepositoryBase;
import com.hillayes.shares.domain.ShareIndex;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ShareIndexRepository extends RepositoryBase<ShareIndex, UUID> {
    public Optional<ShareIndex> findByIsin(String isin) {
        return findFirst("isin", isin);
    }

    public Page<ShareIndex> listAll(int pageNumber, int pageSize) {
        return findByPage(findAll(Sort.by("name")), pageNumber, pageSize);
    }
}
