package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.domain.ShareIndex;

public class SaleExceedsHoldingException extends MensaException {
    public SaleExceedsHoldingException(ShareIndex.ShareIdentity identity, int quantity, int holding) {
        super(SharesErrorCodes.SALE_EXCEEDS_HOLDING);
        addParameter("isin", identity.getIsin());
        addParameter("ticker-symbol", identity.getTickerSymbol());
        addParameter("quantity", Math.abs(quantity));
        addParameter("holding", holding);
    }
}
