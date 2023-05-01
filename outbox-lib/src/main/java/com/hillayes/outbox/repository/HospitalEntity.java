package com.hillayes.outbox.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="message_hospital")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // called by the builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // called by the persistence layer
public class HospitalEntity {
    /**
     * A factory method to create a new hospital for the failed event. This is called by
     * the event dead-letter topic listener.
     *
     * @param eventPacket the event packet that failed delivery and is to be rescheduled.
     * @param reason the reason for the failure.
     * @param cause the cause of the failure.
     */
    public static HospitalEntity fromEventPacket(EventPacket eventPacket,
                                                 String reason, String cause) {
        Instant now = Instant.now();
        return HospitalEntity.builder()
            .eventId(eventPacket.getId())
            .correlationId(eventPacket.getCorrelationId())
            .retryCount(eventPacket.getRetryCount())
            .timestamp(now)
            .reason(reason)
            .cause(cause)
            .topic(eventPacket.getTopic())
            .key(eventPacket.getKey())
            .payloadClass(eventPacket.getPayloadClass())
            .payload(eventPacket.getPayload())
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

    @Enumerated(EnumType.STRING)
    private Topic topic;

    /**
     * The optional key of the event. Events with the same key will be delivered to the
     * same partition. This can ensure the events are delivered in the order in which
     * they were submitted.
     */
    @Column(name = "key", nullable = true)
    private String key;

    @Column(name="payload_class")
    private String payloadClass;

    @Column
    private String payload;
}
