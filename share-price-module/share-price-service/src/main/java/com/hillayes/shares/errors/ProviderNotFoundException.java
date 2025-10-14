package com.hillayes.shares.errors;

import com.hillayes.exception.MensaException;
import com.hillayes.shares.api.domain.ShareProvider;

public class ProviderNotFoundException extends MensaException {
    public ProviderNotFoundException(ShareProvider aProvider) {
        this(aProvider.name());
    }

    public ProviderNotFoundException(String aRailProvider) {
        super(SharesErrorCodes.PROVIDER_NOT_FOUND);
        addParameter("shares-provider", aRailProvider);
    }
}
