package com.hillayes.outbox.repository;

import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.executors.correlation.Correlation;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

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
            .payloadClass(payloadObject.getClass().getName())
            .payload(EventPacket.serialize(payloadObject))
            .build();
    }

    /**
     * A factory method to create a new event entity for the re-delivery of a failed
     * event. This is called by the event dead-letter topic listener.
     * <p>
     * The event's retry count is incremented, and its scheduled delivery is delayed
     * in order to allow the listeners time to recover from any error condition.
     *
     * @param eventPacket the event packet that failed delivery and is to be rescheduled.
     * @param scheduleFor the time at which the event is to be delivered.
     */
    public static EventEntity forRedelivery(EventPacket eventPacket, Instant scheduleFor) {
        Instant now = Instant.now();
        return EventEntity.builder()
            .eventId(eventPacket.getId())
            .correlationId(eventPacket.getCorrelationId())
            .retryCount(eventPacket.getRetryCount() + 1)
            .timestamp(now)
            .scheduledFor(scheduleFor)
            .topic(eventPacket.getTopic())
            .key(eventPacket.getKey())
            .payloadClass(eventPacket.getPayloadClass())
            .payload(eventPacket.getPayload())
            .build();
    }

    /**
     * Converts the EventEntity into an object ready to be delivered by the message
     * broker.
     */
    public EventPacket toEventPacket() {
        return new EventPacket(eventId, topic, correlationId, retryCount, timestamp, key, payloadClass, payload);
    }

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

    @Column(name = "retry_count")
    @Setter
    private int retryCount;

    @Column(name = "timestamp")
    private Instant timestamp;

    /**
     * The date-time on which the event is scheduled to be delivered. This is generally
     * the same as the event's "timestamp"; meaning it is scheduled as soon as possible.
     * However, when an event fails and is to be re-delivered, the scheduled timestamp
     * may be some-time in the future; in order to allow the event listener to recover
     * from any error condition.
     */
    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    private Topic topic;

    /**
     * The optional key of the event. Events with the same key will be delivered to the
     * same partition. This can ensure the events are delivered in the order in which
     * they were submitted.
     */
    @Column(name = "key", nullable = true)
    private String key;

    @Column(name = "payload_class")
    private String payloadClass;

    @Column
    private String payload;
}
