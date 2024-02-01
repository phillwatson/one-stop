package com.hillayes.notification.config;

/**
 * Identifies the template to be used when rendering the content of the email.
 */
public enum TemplateName {
    HEADER,
    USER_REGISTERED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ACCOUNT_ACTIVITY,
    CONSENT_GIVEN,
    CONSENT_DENIED,
    CONSENT_CANCELLED,
    CONSENT_SUSPENDED,
    CONSENT_EXPIRED;
}
