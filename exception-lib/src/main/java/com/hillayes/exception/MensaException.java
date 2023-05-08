package com.hillayes.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MensaException extends RuntimeException {
    private final ErrorCode errorCode;
    private Map<String,Object> context;

    /**
     * Creates a DomainException with the given error code.
     *
     * @param aErrorCode the unique error code identifier.
     */
    public MensaException(ErrorCode aErrorCode)
    {
        this(aErrorCode, null);
    }

    /**
     * Creates a DomainException with the given error code. The cause of the DomainException is also
     * recorded.
     *
     * @param aErrorCode the unique error code identifier.
     * @param aCause the underlying cause of the domain error.
     */
    public MensaException(ErrorCode aErrorCode, Throwable aCause)
    {
        super(aErrorCode.getMessage(), aCause);
        errorCode = aErrorCode;
    }

    /**
     * Returns the error code that uniquely identifies the exception.
     */
    public ErrorCode getErrorCode()
    {
        return errorCode;
    }

    /**
     * Returns the value of the named parameter, or null if the parameter is not set.
     *
     * @param name the name of the parameter to be returned.
     * @return the value of the named parameter, or null if the parameter is not set.
     * @param <T> the expected type of the parameter to be returned.
     */
    public <T> T getParameter(String name) {
        return (T) getContext().get(name);
    }

    /**
     * Adds the given parameter to the exception's context. It will replace any
     * parameter of the same name. If the value is null, the named parameter will
     * be removed from the context.
     *
     * @param name the name of the parameter to be added.
     * @param value the value to be set.
     * @return this exception, to allow method chaining.
     */
    public MensaException addParameter(String name, Object value) {
        if (context == null) {
            if (value == null) {
                return this;
            }
            context = new HashMap<>(5);
        }

        if (value == null) {
            context.remove(name);
        } else {
            context.put(name, value);
        }

        return this;
    }

    protected Map<String, Object> getContext() {
        return context == null ? Collections.emptyMap() : context;
    }
}
