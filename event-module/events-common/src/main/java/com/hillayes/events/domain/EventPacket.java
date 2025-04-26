package com.hillayes.events.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.events.exceptions.EventPayloadDeserializationException;
import com.hillayes.events.exceptions.EventPayloadSerializationException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A packet in which events can be transported from the sender to consumers.
 * The event is represented by the payload property of this class. Other
 * properties are used to manage the delivery of that event to the TopicConsumers.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class EventPacket {
    private static final ObjectMapper MAPPER = MapperFactory.defaultMapper();

    /**
     * The event's unique identifier. This is suitable for testing whether the event
     * has been processed by the consumer - idempotency.
     */
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * The correlation ID of the context/session in which the event was created.
     * This should be carried forward and set within the consumer's context/session.
     */
    private String correlationId;

    /**
     * The number of times the event delivery has been retried, not including the initial
     * delivery. A maximum number of retries can be applied, after which the event is
     * placed on the message-hospital topic.
     */
    @Setter
    private int retryCount;

    /**
     * The timestamp at which the event was created.
     */
    private Instant timestamp;

    /**
     * The topic on which the event is to be delivered. The event will be delivered to
     * all TopicConsumers listening to this topic; that are not in the same consumer
     * group. When multiple TopicConsumers are the same consumer group, only one of
     * those consumers will receive the event.
     */
    private Topic topic;

    /**
     * The optional key of the event. Events with the same key will be delivered to the
     * same partition. This can ensure the events are delivered in the order in which
     * they were submitted - unless they are re-delivered due to errors whilst processing.
     */
    private String key;

    /**
     * The class from which the event's payload was deserialized. This can be used to
     * identify the specific event type within the topic; and allow the payload to be
     * serialized.
     */
    private String payloadClass;

    /**
     * The content of the payload. This is a JSON representation of the class identified by
     * {@link #getPayloadClass()}
     */
    private String payload;

    /**
     * The event payload represented in its original Java class.
     * @see #getPayloadContent()
     */
    @JsonIgnore
    private transient Object payloadContent;

    /**
     * The exception class, if any, that caused the event to be retried.
     */
    private String reason;

    /**
     * The exception message of the exception that caused the event to be retried.
     */
    private String cause;

    /**
     * A no-args constructor for JSON deserialization.
     */
    protected EventPacket() {}

    public EventPacket(UUID id, Topic topic, String correlationId,
                       int retryCount, Instant timestamp,
                       String key, String payloadClass, String payload) {
        this(id, topic, correlationId, retryCount, timestamp, key,
         payloadClass, payload, null, null);
    }

    public EventPacket(UUID id, Topic topic, String correlationId,
                       int retryCount, Instant timestamp,
                       String key, String payloadClass, String payload,
                       String reason, String cause) {
        this.id = id;
        this.topic = topic;
        this.correlationId = correlationId;
        this.retryCount = retryCount;
        this.timestamp = timestamp;
        this.key = key;
        this.payloadClass = payloadClass;
        this.payload = payload;
        this.reason = reason;
        this.cause = cause;
    }

    /**
     * A convenience method to deserialize the payload to the payload class. Attempting to assign the
     * return value to a class that isn't compatible to the {@link #getPayloadClass()} will throw a
     * ClassCastException.
     */
    @JsonIgnore
    public <T> T getPayloadContent() {
        if ((payloadContent == null) && (payload != null)) {
            try {
                payloadContent = MAPPER.readValue(payload, Class.forName(payloadClass));
            } catch (JsonProcessingException | ClassNotFoundException e) {
                throw new EventPayloadDeserializationException(payloadClass, e);
            }
        }
        return (T) payloadContent;
    }

    public static String serialize(Object payloadObject) {
        try {
            return MAPPER.writeValueAsString(payloadObject);
        } catch (JsonProcessingException e) {
            throw new EventPayloadSerializationException(payloadObject.getClass().getName(), e);
        }
    }
}
