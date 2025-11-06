package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.domain.ShareIndex;

public class DuplicateShareIndexException extends MensaException {
    public DuplicateShareIndexException(ShareIndex.ShareIdentity identity, Throwable cause) {
        this(identity.getIsin(), identity.getTickerSymbol(), cause);
    }

    public DuplicateShareIndexException(String isin, String tickerSymbol, Throwable cause) {
        super(SharesErrorCodes.DUPLICATE_SHARE_ISIN, cause);
        addParameter("isin", isin);
        addParameter("ticker-symbol", tickerSymbol);
    }
}
