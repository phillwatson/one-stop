package com.hillayes.shares.api.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.api.domain.ShareProvider;

import java.util.Map;

public class ShareServiceException extends MensaException {
    public ShareServiceException(ShareProvider provider,
                                 String service,
                                 Throwable aCause) {
        super(SharePriceErrorCodes.SHARE_SERVICE_EXCEPTION, aCause);
        addParameter("provider", provider);
        addParameter("service", service);
    }

    public ShareServiceException(ShareProvider provider,
                                 String service,
                                 Throwable aCause,
                                 Map<String,Object> args) {
        this(provider, service, aCause);
        if (args != null) {
            args.forEach(this::addParameter);
        }
    }
}
