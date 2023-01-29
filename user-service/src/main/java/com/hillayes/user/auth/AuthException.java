package com.hillayes.user.auth;

import com.hillayes.exception.ErrorCode;
import com.hillayes.exception.MensaException;

public class AuthException extends MensaException {
    public AuthException(ErrorCode aErrorCode) {
        super(aErrorCode);
    }

    public AuthException(ErrorCode aErrorCode, Throwable aCause) {
        super(aErrorCode, aCause);
    }
}
