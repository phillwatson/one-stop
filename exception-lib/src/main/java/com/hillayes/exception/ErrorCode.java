package com.hillayes.exception;

public interface ErrorCode {
    // some commonly used http status codes
    int BAD_REQUEST_STATUS = 400;
    int UNAUTHORIZED_STATUS = 401;
    int FORBIDDEN_STATUS = 403;
    int NOT_FOUND_STATUS = 404;
    int CONFLICT_STATUS = 409;
    int INTERNAL_SERVER_ERROR_STATUS = 500;

    /**
     * Returns the severity of the error.
     */
    Severity getSeverity();

    /**
     * Returns the error's unique identifier. This can be used by the client to identify the error
     * response and take any mitigating action. It can also be used by the client to look-up a
     * message to be displayed to the user.
     */
    String getId();

    /**
     * Returns the error's default message. This message is not intended for user consumption, but
     * is more for engineer debugging purposes. However, as it may be displayed to the user, the
     * wording used should not be overly technical.
     */
    String getMessage();

    /**
     * The Http status to be returned to the client response.
     */
    int getStatusCode();

    /**
     * Levels of severity for the error.
     */
    enum Severity
    {
        info,
        warn,
        error;
    }
}
