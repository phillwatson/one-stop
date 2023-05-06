package com.hillayes.outbox.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * A repository used to persist events until they are delivered.
 */
@ApplicationScoped
public class HospitalRepository implements PanacheRepositoryBase<HospitalEntity, UUID> {
    /**
     * Returns recorded hospital events in their descending timestamp order.
     *
     * @param page the zero-based page number to be returned.
     * @param pageSize the max number of events to be returned.
     * @return a stream of undelivered events.
     */
    public List<HospitalEntity> list(int page, int pageSize) {
        return find("FROM HospitalEntity", Sort.descending("timestamp"))
                .page(page, pageSize)
                .list();
    }
}
