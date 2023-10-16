package com.hillayes.outbox.sender;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.outbox.repository.EventEntity;
import com.hillayes.outbox.repository.EventRepository;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * The external point of contact for the event outbox.
 */
@ApplicationScoped
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class EventSender {
    @Inject
    private EventRepository eventRepository;

    @Inject
    @TopicObserved(Topic.USER)
    private Event<EventPacket> userEvent;

    @Inject
    @TopicObserved(Topic.USER_AUTH)
    private Event<EventPacket> userAuthEvent;

    @Inject
    @TopicObserved(Topic.CONSENT)
    private Event<EventPacket> consentEvent;

    /**
     * Records the given event for delivery at the next scheduled delivery round.
     * (see EventDeliverer#deliverEvents()).
     * <p>
     * This method should be called within the context of an active transaction.
     * Should that transaction roll-back for any reason, the event will not be
     * delivered. This ensures that events reflect the domain's persisted state.
     *
     * @param topic the topic on which the event will be delivered.
     * @param event the event payload.
     * @param <T> the type of the event payload.
     */
    @Transactional
    public <T> void send(Topic topic, T event) {
        send(topic, null, event);
    }

    /**
     * Records the given event for delivery at the next scheduled delivery round.
     * (see EventDeliverer#deliverEvents()).
     * <p>
     * This method should be called within the context of an active transaction.
     * Should that transaction roll-back for any reason, the event will not be
     * delivered. This ensures that events reflect the domain's persisted state.
     *
     * @param topic the topic on which the event will be delivered.
     * @param key the key to use when sending events that MUST be delivered in the
     *     order submitted.
     * @param event the event payload.
     * @param <T> the type of the event payload.
     */
    @Transactional
    public <K, T> void send(Topic topic, K key, T event) {
        log.debug("Sending event [topic: {}, payload: {}]", topic, event.getClass().getName());
        EventEntity entity = EventEntity.forInitialDelivery(topic, key, event);
        eventRepository.persist(entity);

        EventPacket eventPacket = entity.toEventPacket();
        switch (topic) {
            case USER:
                userEvent.fire(eventPacket);
                break;
            case USER_AUTH:
                userAuthEvent.fire(eventPacket);
                break;
            case CONSENT:
                consentEvent.fire(eventPacket);
                break;
        }
    }
}
