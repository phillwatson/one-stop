package com.hillayes.events.exceptions;

public class EventPayloadDeserializationException extends RuntimeException {
    public EventPayloadDeserializationException(String className, Throwable cause) {
        super(String.format("Failed to deserialize payload [class: %1$s]", className), cause);
    }
}
