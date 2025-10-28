package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;

public class SaleExceedsHoldingException extends MensaException {
    public SaleExceedsHoldingException(String isin, int quantity, int holding) {
        super(SharesErrorCodes.SALE_EXCEEDS_HOLDING);
        addParameter("isin", isin);
        addParameter("quantity", Math.abs(quantity));
        addParameter("holding", holding);
    }
}
