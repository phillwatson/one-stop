package com.hillayes.executors.exceptions;

public class TaskPayloadDeserializationException extends RuntimeException {
    public TaskPayloadDeserializationException(String className, Throwable cause) {
        super(String.format("Failed to deserialize payload [class: %1$s]", className), cause);
    }
}
