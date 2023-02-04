package com.hillayes.user.errors;

import com.hillayes.exception.MensaException;

public class DuplicateUsernameException extends MensaException {
    public DuplicateUsernameException(String aUsername) {
        super(UserErrorCodes.USERNAME_ALREADY_EXISTS);
        addParameter("username", aUsername);
    }
}
