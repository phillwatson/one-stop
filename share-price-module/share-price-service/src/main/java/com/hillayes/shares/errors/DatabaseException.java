package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;

public class DatabaseException extends MensaException {
    public DatabaseException(Throwable cause) {
        super(SharesErrorCodes.DATABASE_ERROR, cause);
    }
}
