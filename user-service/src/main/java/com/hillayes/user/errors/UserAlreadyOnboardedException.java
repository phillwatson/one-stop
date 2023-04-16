package com.hillayes.user.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.user.domain.User;

public class UserAlreadyOnboardedException extends MensaException {
    public UserAlreadyOnboardedException(User user) {
        super(UserErrorCodes.USER_ALREADY_ONBOARDED);
        addParameter("username", user.getUsername());
    }
}
