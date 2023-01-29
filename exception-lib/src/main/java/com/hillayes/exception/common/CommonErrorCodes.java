package com.hillayes.exception.common;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum CommonErrorCodes implements ErrorCode {
    ENTITY_NOT_FOUND(Severity.info, "The identified entity cannot be found.", NOT_FOUND_STATUS),
    PARAMETER_MISSING(Severity.info, "The named parameter is missing from request.", BAD_REQUEST_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    CommonErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}
