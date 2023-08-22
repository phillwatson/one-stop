package com.hillayes.executors.exceptions;

public class JobPayloadSerializationException extends RuntimeException {
    public JobPayloadSerializationException(String className, Throwable cause) {
        super(String.format("Failed to serialize payload [class: %1$s]", className), cause);
    }
}
