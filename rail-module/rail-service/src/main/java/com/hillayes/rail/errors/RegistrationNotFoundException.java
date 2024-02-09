package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.api.domain.ConsentResponse;
import com.hillayes.rail.api.domain.RailProvider;

public class RegistrationNotFoundException extends MensaException {
    public RegistrationNotFoundException(RailProvider aRailProvider, ConsentResponse aResponse) {
        super(RailsErrorCodes.REGISTRATION_NOT_FOUND);
        addParameter("railProvider", aRailProvider);
        addParameter("consentReference", aResponse.getConsentReference());
        if (aResponse.isError()) {
            addParameter("errorCode", aResponse.getErrorCode());
            addParameter("errorDescription", aResponse.getErrorDescription());
        }
    }
}
