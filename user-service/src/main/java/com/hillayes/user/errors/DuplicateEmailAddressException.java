package com.hillayes.user.errors;

import com.hillayes.exception.MensaException;

public class DuplicateEmailAddressException extends MensaException {
    public DuplicateEmailAddressException(String aEmailAddress) {
        super(UserErrorCodes.EMAIL_ALREADY_EXISTS);
        addParameter("email", aEmailAddress);
    }

    /**
     * Returns the email address that already exists.
     */
    public String getEmail() {
        return getParameter("email");
    }
}
