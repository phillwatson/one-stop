package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;

public class DuplicateIsinException extends MensaException {
    public DuplicateIsinException(String isin) {
        super(SharesErrorCodes.DUPLICATE_SHARE_ISIN);
        addParameter("isin", isin);
    }
}
