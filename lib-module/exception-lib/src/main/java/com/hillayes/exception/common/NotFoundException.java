package com.hillayes.exception.common;

import com.hillayes.exception.MensaException;

public class NotFoundException extends MensaException {
    public NotFoundException(String aEntityType, Object aEntityId) {
        super(CommonErrorCodes.ENTITY_NOT_FOUND);
        addParameter("entity-type", aEntityType);
        addParameter("entity-id", aEntityId);
    }
}
