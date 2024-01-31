package com.hillayes.rail.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum RailsErrorCodes implements ErrorCode {
    BANK_ALREADY_REGISTERED(Severity.info, "You have already registered that bank.", CONFLICT_STATUS),
    BANK_REGISTRATION_FAILED(Severity.info, "Failed to register the bank.", INTERNAL_SERVER_ERROR_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    RailsErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}
