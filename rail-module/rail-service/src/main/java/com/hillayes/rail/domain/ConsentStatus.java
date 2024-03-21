package com.hillayes.rail.domain;

/**
 * The status of a consent. The order of the enum values is important as it is
 * used to select which consent is returned when a user holds multiple consents
 * for the same institution.
 */
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
