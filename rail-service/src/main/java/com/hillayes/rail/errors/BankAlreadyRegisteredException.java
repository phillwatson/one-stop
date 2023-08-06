package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;

import java.util.UUID;

public class BankAlreadyRegisteredException extends MensaException {
    public BankAlreadyRegisteredException(UUID aUserId, String aInstitution) {
        super(RailsErrorCodes.BANK_ALREADY_REGISTERED);
        addParameter("user-id", aUserId);
        addParameter("institution-id", aInstitution);
    }
}
