package com.hillayes.outbox.repository;

import com.hillayes.commons.jpa.RepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * A repository used to persist events until they are delivered.
 */
@ApplicationScoped
public class EventRepository extends RepositoryBase<EventEntity, UUID> {
    /**
     * Returns scheduled events in the order they are scheduled. The events are locked
     * to prevent allow update and prevent update conflicts.
     *
     * @param batchSize the max number of events to be returned.
     * @return a stream of undelivered events.
     */
    public List<EventEntity> listUndelivered(int batchSize) {
        return lock("FROM EventEntity WHERE scheduledFor < CURRENT_TIMESTAMP ORDER BY scheduledFor", batchSize);
    }
}
