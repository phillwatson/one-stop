package com.hillayes.events.exceptions;

public class EventPayloadSerializationException extends RuntimeException {
    public EventPayloadSerializationException(String className, Throwable cause) {
        super(String.format("Failed to serialize payload [class: %1$s]", className), cause);
    }
}
