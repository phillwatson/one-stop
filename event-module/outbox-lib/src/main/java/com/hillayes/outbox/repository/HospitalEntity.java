package com.hillayes.outbox.repository;

import com.hillayes.events.domain.Topic;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="message_hospital")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // called by the builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // called by the persistence layer
public class HospitalEntity {
    /**
     * A factory method to create a new hospital for the failed event.
     *
     * @param event the event that failed delivery and is to be rescheduled.
     * @param error the error that caused the event to fail.
     */
    public static HospitalEntity fromEventEntity(EventEntity event,
                                                 String consumer,
                                                 Throwable error) {
        Instant now = Instant.now();
        String reason = error.getClass().getName();
        String cause = error.getMessage();

        return HospitalEntity.builder()
            .eventId(event.getId())
            .correlationId(event.getCorrelationId())
            .retryCount(event.getRetryCount())
            .timestamp(now)
            .reason(reason)
            .cause(cause)
            .consumer(consumer)
            .topic(event.getTopic())
            .key(event.getKey())
            .payloadClass(event.getPayloadClass())
            .payload(event.getPayload())
            .build();
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
    @Column(name="correlation_id")
    private String correlationId;

    @Column(name="retry_count")
    private int retryCount;

    private Instant timestamp;

    private String reason;

    private String cause;

    /**
     * The consumer class that raised the error.
     */
    private String consumer;

    @Enumerated(EnumType.STRING)
    private Topic topic;

    /**
     * The optional key of the event. Events with the same key will be delivered to the
     * same partition. This can ensure the events are delivered in the order in which
     * they were submitted.
     */
    @Column(name = "key", nullable = true)
    private String key;

    @Column(name="payload_class", nullable = true)
    private String payloadClass;

    @Column(nullable = true)
    private String payload;
}
