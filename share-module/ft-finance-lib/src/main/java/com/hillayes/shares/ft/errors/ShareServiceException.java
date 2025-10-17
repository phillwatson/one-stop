package com.hillayes.shares.ft.errors;

import com.hillayes.exception.MensaException;

import java.util.Map;

public class ShareServiceException extends MensaException {
    public ShareServiceException(String service, Throwable aCause) {
        super(SharePriceErrorCodes.SHARE_SERVICE_EXCEPTION, aCause);
        addParameter("service", service);
    }

    public ShareServiceException(String service, Throwable aCause, Map<String,Object> args) {
        this(service, aCause);
        if (args != null) {
            args.forEach(this::addParameter);
        }
    }
}
