package com.hillayes.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * The response payload generated by the ExceptionMapper and returned to the client
 * when an exception occurs. Its properties are taken from the exception's ErrorCode
 * and context attributes.
 */
@Data
public class ServiceError {
    /**
     * a unique value that identifies the request on which the error occurred.
     */
    private String correlationId;

    /**
     * an indication of the error's severity.
     */
    private ErrorCode.Severity severity;

    /**
     * a unique identity of the error type; so that the client may easily identify it and
     * process it accordingly.
     */
    private String messageId;

    /**
     * a readable message that describes the error but is not intended for user consumption.
     */
    private String message;

    // the collection of name/value pairs that give context to the error.
    // these might be used within the error message displayed to the user.
    private Map<String,String> contextAttributes = new HashMap<>();

    /**
     * A simple name/value tuple that can be used to convey an attribute value for context within
     * the error message.
     */
    @Data
    @AllArgsConstructor
    public static class Parameter {
        private String name;
        private Object value;
    }
}