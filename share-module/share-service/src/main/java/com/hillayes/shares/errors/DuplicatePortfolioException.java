package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;

public class DuplicatePortfolioException extends MensaException {
    public DuplicatePortfolioException(String portfolioName, Throwable cause) {
        super(SharesErrorCodes.DUPLICATE_PORTFOLIO, cause);
        addParameter("portfolio-name", portfolioName);
    }
}
