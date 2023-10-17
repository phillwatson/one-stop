package com.hillayes.exception.common;

import com.hillayes.exception.MensaException;

public class MissingParameterException extends MensaException {
    public MissingParameterException(String aParameterName) {
        super(CommonErrorCodes.PARAMETER_MISSING);
        addParameter("parameter-name", aParameterName);
    }
}
