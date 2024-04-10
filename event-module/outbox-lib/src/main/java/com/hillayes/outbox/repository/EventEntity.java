package com.hillayes.outbox.repository;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.commons.correlation.Correlation;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * The persisted representation of an event, as managed by the outbox. Whilst
 * events await their delivery they will be persisted in the event table. The
 * outbox delivery will periodically read a batch of these records (in the
 * order they were written) and deliver them to the TopicConsumers.
 */
@Entity
@Table(name = "events")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // called by the persistence layer
@AllArgsConstructor(access = AccessLevel.PRIVATE) // called by the builder
@Builder(access = AccessLevel.PRIVATE)
public class EventEntity {
    /**
     * A factory method to create a new event entity for its initial delivery.
     * Called when an event is first submitted delivery.
     *
     * @param topic the topic on which the event is to be delivered.
     * @param key the optional key of the event. Used to ensure that events with
     *     the same key are delivered in order.
     * @param payloadObject the payload to be passed in the event.
     */
    public static EventEntity forInitialDelivery(Topic topic, Object key, Object payloadObject) {
        Instant now = Instant.now();
        return EventEntity.builder()
            .eventId(UUID.randomUUID())
            .correlationId(Correlation.getCorrelationId().orElse(UUID.randomUUID().toString()))
            .retryCount(0)
            .timestamp(now)
            .scheduledFor(now)
            .topic(topic)
            .key(key == null ? null : key.toString())
            .payloadClass(payloadObject == null ? null : payloadObject.getClass().getName())
            .payload(EventPacket.serialize(payloadObject))
            .build();
    }

    /**
     * A factory method to create a new event entity for the re-delivery of a failed
     * event.
     * <p>
     * The event's retry count is incremented, and its scheduled delivery is delayed
     * in order to allow the listeners time to recover from any error condition.
     *
     * @param event the event that failed delivery and is to be rescheduled.
     * @param error the error that caused the event to fail.
     * @param scheduleFor the time at which the event is to be delivered.
     */
    public static EventEntity forRedelivery(EventEntity event,
                                            Throwable error,
                                            Instant scheduleFor) {
        String reason = error.getClass().getName();
        String cause = error.getMessage();

        event.setRetryCount(event.getRetryCount() + 1);
        event.setScheduledFor(scheduleFor);
        event.setReason(reason);
        event.setCause(cause);
        return event;
    }

    /**
     * Converts the EventEntity into an object ready to be delivered by the message
     * broker.
     */
    public EventPacket toEventPacket() {
        return new EventPacket(eventId, topic, correlationId, retryCount, timestamp, key,
            payloadClass, payload, reason, cause);
    }

    /**
     * The identifier used by the event outbox persistence.
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * The event's own unique identifier. This is suitable for testing whether the event
     * has been processed by the consumer - idempotency.
     */
    @Column(name = "event_id")
    private UUID eventId;

    /**
     * The correlation-id assigned to the event when it was first submitted for delivery.
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * The date-time at which the event was originally posted onto the topic queue.
     */
    @Column(name = "timestamp")
    private Instant timestamp;

    /**
     * The number of times the event delivery has been retried, not including the initial
     * delivery. A maximum number of retries can be applied, after which the event is
     * placed on the message-hospital topic.
     */
    @Column(name = "retry_count")
    @Setter(AccessLevel.PRIVATE)
    private int retryCount;

    /**
     * The date-time on which the event is scheduled to be delivered. This is generally
     * the same as the event's "timestamp"; meaning it is scheduled as soon as possible.
     * However, when an event fails and is to be re-delivered, the scheduled timestamp
     * may be some-time in the future; in order to allow the event listener to recover
     * from any error condition.
     */
    @Column(name = "scheduled_for")
    @Setter(AccessLevel.PRIVATE)
    private Instant scheduledFor;

    /**
     * The topic on which the event is to be delivered. The event will be delivered to
     * all TopicConsumers listening to this topic; that are not in the same consumer
     * group. When multiple TopicConsumers are the same consumer group, only one of
     * those consumers will receive the event.
     */
    @Enumerated(EnumType.STRING)
    private Topic topic;

    /**
     * The optional key of the event. Events with the same key will be delivered to the
     * same partition. This can ensure the events are delivered in the order in which
     * they were submitted - unless they are re-delivered due to errors whilst processing.
     */
    @Column(name = "key", nullable = true)
    private String key;

    /**
     * The class from which the event's payload was deserialized. This can be used to
     * identify the specific event type within the topic; and allow the payload to be
     * serialized.
     */
    @Column(name = "payload_class", nullable = true)
    private String payloadClass;

    /**
     * A representation of the event's payload as a JSON packet. The class named by
     * the payloadClass allows this payload to be serialized.
     */
    @Column(nullable = true)
    private String payload;

    @Column(nullable = true)
    @Setter(AccessLevel.PRIVATE)
    private String reason;

    @Column(nullable = true)
    @Setter(AccessLevel.PRIVATE)
    private String cause;

    /**
     * The consumer class that raised the error.
     */
    @Column(nullable = true)
    @Setter(AccessLevel.PRIVATE)
    private String consumer;
}
