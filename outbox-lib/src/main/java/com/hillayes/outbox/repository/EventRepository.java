package com.hillayes.outbox.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EventRepository implements PanacheRepositoryBase<EventEntity, UUID> {
    /**
     * Returns the undelivered events in the order they were posted. The events are locked to prevent
     * allow update and prevent update conflicts.
     *
     * @param batchSize the max number of events to be returned.
     * @return a stream of undelivered events.
     */
    public List<EventEntity> listUndelivered(int batchSize) {
        return find("FROM EventEntity WHERE deliveredAt IS NULL ORDER BY timestamp")
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .page(0, batchSize)
                .list();
    }
}
