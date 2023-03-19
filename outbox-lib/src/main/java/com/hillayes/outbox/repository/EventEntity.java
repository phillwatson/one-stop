package com.hillayes.outbox.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.executors.correlation.Correlation;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="events")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // called by the persistence layer
@AllArgsConstructor(access = AccessLevel.PRIVATE) // called by the builder
@Builder(access  = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEntity {
    /**
     * A factory method to create a new event entity for its initial delivery.
     * Called when an event is first submitted delivery.
     *
     * @param topic the topic on which the event is to be delivered.
     * @param payloadObject the payload to be passed in the event.
     */
    public static EventEntity forInitialDelivery(Topic topic, Object payloadObject) {
        Instant now = Instant.now();
        return EventEntity.builder()
            .correlationId(Correlation.getCorrelationId().orElse(UUID.randomUUID().toString()))
            .retryCount(0)
            .timestamp(now)
            .scheduledFor(now)
            .topic(topic)
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
     */
    public static EventEntity forRedelivery(EventPacket eventPacket) {
        Instant now = Instant.now();
        return EventEntity.builder()
            .correlationId(eventPacket.getCorrelationId())
            .retryCount(eventPacket.getRetryCount() + 1)
            .timestamp(now)
            .scheduledFor(now.plusSeconds(60L * eventPacket.getRetryCount() + 1))
            .topic(eventPacket.getTopic())
            .payloadClass(eventPacket.getPayloadClass())
            .payload(eventPacket.getPayload())
            .build();
    }

    /**
     * Converts the EventEntity into an object ready to be delivered by the message
     * broker.
     */
    public EventPacket toEntityPacket() {
        return new EventPacket(id, topic, correlationId, retryCount, timestamp, payloadClass, payload);
    }

    @Id
    @GeneratedValue(generator = "uuid2")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name="correlation_id")
    private String correlationId;

    @Column(name="retry_count")
    @Setter
    private int retryCount;

    @Column(name="timestamp")
    private Instant timestamp;

    /**
     * The date-time on which the event is scheduled to be delivered. This is generally
     * the same as the event's "timestamp"; meaning it is scheduled as soon as possible.
     * However, when an event fails and is to be re-delivered, the scheduled timestamp
     * may be some-time in the future; in order to allow the event listener to recover
     * from any error condition.
     */
    @Column(name="scheduled_for")
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    private Topic topic;

    @Column(name="payload_class")
    private String payloadClass;

    private String payload;
}
