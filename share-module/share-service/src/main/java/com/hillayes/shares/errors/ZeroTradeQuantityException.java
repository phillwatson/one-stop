package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.domain.ShareIndex;

public class ZeroTradeQuantityException extends MensaException {
    public ZeroTradeQuantityException(ShareIndex shareIndex) {
        super(SharesErrorCodes.ZERO_TRADE_QUANTITY);
        addParameter("isin", (shareIndex == null) ? "unknown" : shareIndex.getIdentity().getIsin());
        addParameter("ticker-symbol", (shareIndex == null) ? "unknown" : shareIndex.getIdentity().getTickerSymbol());
    }
}
