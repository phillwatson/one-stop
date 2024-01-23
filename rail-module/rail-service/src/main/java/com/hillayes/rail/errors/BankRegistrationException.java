package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;

import java.util.UUID;

public class BankRegistrationException extends MensaException {
    public BankRegistrationException(UUID aUserId, String aInstitution, Throwable aCause) {
        super(RailsErrorCodes.BANK_REGISTRATION_FAILED, aCause);
        addParameter("userId", aUserId);
        addParameter("institution", aInstitution);
    }
}
