package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;

import java.util.UUID;

public class BankRegistrationException extends MensaException {
    public BankRegistrationException(UUID aUserId, String aInstitutionId, Throwable aCause) {
        super(RailsErrorCodes.BANK_REGISTRATION_FAILED, aCause);
        addParameter("user-id", aUserId);
        addParameter("institution-id", aInstitutionId);
    }
}
