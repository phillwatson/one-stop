package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.domain.ShareIndex;

public class ZeroTradeQuantityException extends MensaException {
    public ZeroTradeQuantityException(ShareIndex.ShareIdentity identity) {
        super(SharesErrorCodes.ZERO_TRADE_QUANTITY);
        addParameter("isin", identity.getIsin());
        addParameter("ticker-symbol", identity.getTickerSymbol());
    }
}
