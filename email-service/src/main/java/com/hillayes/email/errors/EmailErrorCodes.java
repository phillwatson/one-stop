package com.hillayes.email.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum EmailErrorCodes implements ErrorCode {
    EMAIL_TEMPLATE_NOT_FOUND(Severity.error, "Unable to locate email template.", INTERNAL_SERVER_ERROR_STATUS),
    EMAIL_TEMPLATE_READ(Severity.error, "Unexpected error whilst reading email template.", INTERNAL_SERVER_ERROR_STATUS),
    FAILED_TO_SEND_EMAIL(Severity.info, "Failed to send email.", INTERNAL_SERVER_ERROR_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    EmailErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}
