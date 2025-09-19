package com.hillayes.executors.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.executors.exceptions.TaskPayloadDeserializationException;
import com.hillayes.executors.exceptions.TaskPayloadSerializationException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A data-class that is persisted with to NamedAdhocTask's queue. It records
 * the payload to be processed by the NamedAdhocTask, and the correlation ID
 * that was active at the time the task was queued. This allows the correlation
 * ID to be re-activated when the task is processed.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class AdhocTaskData {
    private static final ObjectMapper MAPPER = MapperFactory.defaultMapper();

    /**
     * The correlation ID of the context/session in which the event was created.
     * This should be carried forward and set within the consumer's context/session.
     */
    String correlationId;

    /**
     * The number times the task has been repeated due to an INCOMPLETE result.
     */
    int repeatCount;

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

    public AdhocTaskData(String correlationId, Object payload) {
        this.repeatCount = 0;
        this.correlationId = correlationId;

        if (payload != null) {
            this.payloadClass = payload.getClass().getName();
            this.payload = serialize(payload);
        }
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
                throw new TaskPayloadDeserializationException(payloadClass, e);
            }
        }
        return (T) payloadContent;
    }

    private String serialize(Object payloadObject) {
        try {
            return MAPPER.writeValueAsString(payloadObject);
        } catch (JsonProcessingException e) {
            throw new TaskPayloadSerializationException(payloadObject.getClass().getName(), e);
        }
    }
}
