package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.domain.UserConsent;

public class DeleteRailConsentException extends MensaException {
    public DeleteRailConsentException(UserConsent userConsent, Throwable cause) {
        super(RailsErrorCodes.FAILED_TO_DELETE_RAIL_CONSENT, cause);
        addParameter("institution-id", userConsent.getInstitutionId());
        addParameter("rail-provider", userConsent.getProvider());
        addParameter("consent-id", userConsent.getId());
    }
}
