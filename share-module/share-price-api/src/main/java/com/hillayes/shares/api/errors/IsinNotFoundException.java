package com.hillayes.shares.api.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.api.domain.ShareProvider;

public class IsinNotFoundException extends MensaException {
    public IsinNotFoundException(ShareProvider provider, String aIsin) {
        this(provider, aIsin, null);
    }

    public IsinNotFoundException(ShareProvider provider, String aIsin, Throwable aCause) {
        super(SharePriceErrorCodes.ISIN_NOT_FOUND, aCause);
        addParameter("provider", provider);
        addParameter("isin", aIsin);
    }
}
