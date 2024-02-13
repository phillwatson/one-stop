package com.hillayes.rail.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.rail.api.domain.RailProvider;

public class RailNotFoundException extends MensaException {
    public RailNotFoundException(RailProvider aRailProvider) {
        super(RailsErrorCodes.RAIL_NOT_FOUND);
        addParameter("rail-provider", aRailProvider);
    }
}
