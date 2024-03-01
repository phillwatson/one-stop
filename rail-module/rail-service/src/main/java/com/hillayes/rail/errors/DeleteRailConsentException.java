package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.domain.UserConsent;

public class DeleteRailConsentException extends MensaException {
    public DeleteRailConsentException(UserConsent userConsent) {
        super(RailsErrorCodes.FAILED_TO_DELETE_RAIL_CONSENT);
        addParameter("rail-provider", userConsent.getProvider());
        addParameter("consent-id", userConsent.getId());
    }
}
