package com.hillayes.executors.exceptions;

public class JobPayloadDeserializationException extends RuntimeException {
    public JobPayloadDeserializationException(String className, Throwable cause) {
        super(String.format("Failed to deserialize payload [class: %1$s]", className), cause);
    }
}
