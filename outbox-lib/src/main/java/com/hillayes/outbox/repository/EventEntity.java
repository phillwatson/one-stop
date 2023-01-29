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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEntity {
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

    @Column(name="delivered_at")
    @Setter
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    private Topic topic;

    @Column(name="payload_class")
    private String payloadClass;

    private String payload;

    public EventEntity(Topic topic, Object payloadObject) {
        this(topic, payloadObject, Correlation.getCorrelationId().orElse(UUID.randomUUID().toString()), Instant.now());
    }

    public EventEntity(Topic topic, Object payloadObject, String correlationId, Instant timestamp) {
        this.correlationId = correlationId;
        this.retryCount = 0;
        this.timestamp = timestamp;
        this.topic = topic;
        this.payloadClass = payloadObject.getClass().getName();
        this.payload = EventPacket.serialize(payloadObject);
    }

    public EventPacket toEntityPacket() {
        return new EventPacket(id, topic, correlationId, retryCount, timestamp, payloadClass, payload);
    }
}
