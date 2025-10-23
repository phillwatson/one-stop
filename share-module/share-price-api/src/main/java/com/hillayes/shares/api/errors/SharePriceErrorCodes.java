package com.hillayes.shares.api.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SharePriceErrorCodes implements ErrorCode {
    SHARE_SERVICE_EXCEPTION(Severity.error, "Failed to contact share service", INTERNAL_SERVER_ERROR_STATUS),
    ISIN_NOT_FOUND(Severity.info, "Failed to locate entry for given ISIN.", NOT_FOUND_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    SharePriceErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}
