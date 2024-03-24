package com.hillayes.rail.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum RailsErrorCodes implements ErrorCode {
    BANK_ALREADY_REGISTERED(Severity.info, "You have already registered that bank.", CONFLICT_STATUS),
    BANK_REGISTRATION_FAILED(Severity.error, "Failed to register the bank.", INTERNAL_SERVER_ERROR_STATUS),
    REGISTRATION_NOT_FOUND(Severity.info, "Unable to identify registration consent record.", INTERNAL_SERVER_ERROR_STATUS),
    RAIL_NOT_FOUND(Severity.error, "Unable to identify rail.", INTERNAL_SERVER_ERROR_STATUS),
    FAILED_TO_DELETE_RAIL_CONSENT(Severity.error, "Failed to delete consent.", INTERNAL_SERVER_ERROR_STATUS);

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
