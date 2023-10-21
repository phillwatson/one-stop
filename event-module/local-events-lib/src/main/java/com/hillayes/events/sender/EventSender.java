package com.hillayes.events.sender;

import com.hillayes.events.annotation.TopicObserved;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends events to the internal topic observers. A typical observer may be written
 * as follows:
 * <pre>
 * /@ApplicationScoped
 * public class UserTopicConsumer {
 *   /@Transactional(Transactional.TxType.REQUIRES_NEW)
 *   public void consume(/@Observes(during = TransactionPhase.AFTER_SUCCESS)
 *                       /@TopicObserved(Topic.USER) EventPacket eventPacket) {
 *   ...
 *   }
 * }
 * </pre>
 */
@ApplicationScoped
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class EventSender {
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
     * Records the given event for delivery to the registered Observers.
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
     * Issues the given event for delivery.
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

        EventPacket eventPacket = EventPacket.forInitialDelivery(topic, key, event);
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
