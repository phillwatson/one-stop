package com.hillayes.user.errors;

import com.hillayes.exception.MensaException;

public class UserRegistrationException extends MensaException {
    public UserRegistrationException(String email) {
        super(UserErrorCodes.USER_REGISTRATION_ERROR);
        addParameter("email", email);
    }
}
