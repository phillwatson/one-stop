package com.hillayes.events.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hillayes.events.exceptions.EventPayloadDeserializationException;
import com.hillayes.events.exceptions.EventPayloadSerializationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A packet in which events can be transported from the sender to consumers.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventPacket {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * The event's unique identifier. This is suitable for testing whether the event
     * has been processed by the consumer - idempotency.
     */
    private UUID id;

    /**
     * The correlation ID of the context/session in which the event was created.
     * This should be carried forward and set within the consumer's context/session.
     */
    private String correlationId;

    /**
     * The number of times the delivery of this event has failed.
     */
    @Setter
    private int retryCount;

    /**
     * The timestamp at which the event was created.
     */
    private Instant timestamp;

    /**
     * The topic on which the event was sent. This will match the topic on which the consumer is listening.
     */
    private Topic topic;

    /**
     * The class of the payload data. Allows the payload to be deserialized.
     */
    private String payloadClass;

    /**
     * The content of the payload. This is a JSON representation of the class identified by
     * {@link #getPayloadClass()}
     */
    private String payload;

    @JsonIgnore
    private Object payloadContent;

    public EventPacket(UUID id, Topic topic, String correlationId, int retryCount, Instant timestamp,
                       String payloadClass, String payload) {
        this.id = id;
        this.topic = topic;
        this.correlationId = correlationId;
        this.retryCount = retryCount;
        this.timestamp = timestamp;
        this.payloadClass = payloadClass;
        this.payload = payload;
    }

    /**
     * A convenience method to deserialize the payload to the payload class. Attempting to assign the
     * return value to a class that isn't compatible to the {@link #getPayloadClass()} will throw a
     * ClassCastException.
     */
    @JsonIgnore
    public <T> T getPayloadContent() {
        if (payloadContent == null) {
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
