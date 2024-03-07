package com.hillayes.notification.config;

/**
 * Identifies the template to be used when rendering the content of the email.
 * A template folder should exist for each TemplateName enum value; using the
 * enum name in lower, snake-case format.
 */
public enum TemplateName {
    HEADER,
    USER_REGISTERED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ACCOUNT_ACTIVITY,
    NEW_OIDC_LOGIN,
    CONSENT_GIVEN,
    CONSENT_DENIED,
    CONSENT_CANCELLED,
    CONSENT_SUSPENDED,
    CONSENT_EXPIRED;
}
