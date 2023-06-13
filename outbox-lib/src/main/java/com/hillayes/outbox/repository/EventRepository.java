package com.hillayes.outbox.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

/**
 * A repository used to persist events until they are delivered.
 */
@ApplicationScoped
public class EventRepository implements PanacheRepositoryBase<EventEntity, UUID> {
    /**
     * Returns scheduled events in the order they are scheduled. The events are locked
     * to prevent allow update and prevent update conflicts.
     *
     * @param batchSize the max number of events to be returned.
     * @return a stream of undelivered events.
     */
    public List<EventEntity> listUndelivered(int batchSize) {
        return find("FROM EventEntity WHERE scheduledFor < CURRENT_TIMESTAMP ORDER BY scheduledFor")
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .page(0, batchSize)
                .list();
    }
}
