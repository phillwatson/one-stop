package com.hillayes.rail.domain;

public enum ConsentStatus {
    // consent has been registered and awaiting user acceptance
    INITIATED,

    // user and rail have accepted the consent
    GIVEN,

    // user and rail have rejected the consent
    DENIED,

    // the consent has expired
    EXPIRED,

    // the consent has been suspended
    SUSPENDED,

    // the consent has been cancelled by the user
    CANCELLED,

    // the consent has been timed out whilst waiting for user acceptance
    TIMEOUT;
}
