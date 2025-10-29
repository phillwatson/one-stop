package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;

public class ZeroTradeQuantityException extends MensaException {
    public ZeroTradeQuantityException(String isin) {
        super(SharesErrorCodes.ZERO_TRADE_QUANTITY);
        addParameter("isin", isin);
    }
}
