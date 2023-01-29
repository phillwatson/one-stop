package com.hillayes.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
    private List<Parameter> contextAttributes = new ArrayList<>();

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
