package com.hillayes.shares.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SharesErrorCodes implements ErrorCode {
    PROVIDER_NOT_FOUND(Severity.error, "Unable to identify share provider.", INTERNAL_SERVER_ERROR_STATUS),
    DATABASE_ERROR(Severity.error, "Unexpected database error", INTERNAL_SERVER_ERROR_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    SharesErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}
