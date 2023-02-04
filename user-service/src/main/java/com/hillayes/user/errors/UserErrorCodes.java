package com.hillayes.user.errors;

import com.hillayes.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum UserErrorCodes implements ErrorCode {
    USERNAME_ALREADY_EXISTS(Severity.info, "The given username is already in use.", CONFLICT_STATUS);

    private final Severity severity;
    private final String message;
    private final int statusCode;

    UserErrorCodes(Severity severity, String message, int statusCode) {
        this.severity = severity;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getId() {
        return name();
    }
}