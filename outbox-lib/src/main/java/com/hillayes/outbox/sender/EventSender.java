package com.hillayes.outbox.sender;

import com.hillayes.events.domain.Topic;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

/**
 * The external point of contact for the event outbox.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class EventSender {
    private final EventRepository eventRepository;

    /**
     * Records the given event for delivery at the next scheduled delivery round.
     * (see EventDeliverer#deliverEvents()).
     * <p>
     * This method must be called within the context of an active transaction.
     * Should that transaction roll-back for any reason, the event will not be
     * delivered. This ensures that events reflect the domain's persisted state.
     *
     * @param topic the topic on which the event will be delivered.
     * @param event the event payload.
     * @param <T> the type of the event payload.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public <T> void send(Topic topic, T event) {
        log.debug("Sending event [payload: {}]", event.getClass().getName());
        eventRepository.persist(EventEntity.forInitialDelivery(topic, event));
    }
}
