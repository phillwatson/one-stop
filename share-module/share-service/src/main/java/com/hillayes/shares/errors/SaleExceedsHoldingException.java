package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.domain.ShareIndex;

public class SaleExceedsHoldingException extends MensaException {
    public SaleExceedsHoldingException(ShareIndex shareIndex, int quantity, int holding) {
        super(SharesErrorCodes.SALE_EXCEEDS_HOLDING);
        addParameter("isin", shareIndex.getIdentity().getIsin());
        addParameter("ticker-symbol", shareIndex.getIdentity().getTickerSymbol());
        addParameter("quantity", Math.abs(quantity));
        addParameter("holding", holding);
    }
}
