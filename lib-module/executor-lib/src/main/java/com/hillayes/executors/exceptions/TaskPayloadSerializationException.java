package com.hillayes.executors.exceptions;

public class TaskPayloadSerializationException extends RuntimeException {
    public TaskPayloadSerializationException(String className, Throwable cause) {
        super(String.format("Failed to serialize payload [class: %1$s]", className), cause);
    }
}
