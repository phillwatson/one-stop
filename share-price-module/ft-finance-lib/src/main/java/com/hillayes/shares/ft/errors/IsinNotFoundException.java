package com.hillayes.shares.ft.errors;

import com.hillayes.exception.MensaException;

public class IsinNotFoundException extends MensaException {
    public IsinNotFoundException(String aIsin) {
        this(aIsin, null);
    }

    public IsinNotFoundException(String aIsin, Throwable aCause) {
        super(SharePriceErrorCodes.ISIN_NOT_FOUND, aCause);
        addParameter("isin", aIsin);
    }
}
