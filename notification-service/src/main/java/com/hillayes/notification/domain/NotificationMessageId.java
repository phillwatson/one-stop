package com.hillayes.notification.domain;

public enum NotificationMessageId {
    CONSENT_EXPIRED(NotificationSeverity.warn, "Your consent has expired. Please renew it.");

    private final NotificationSeverity severity;
    private final String summary;

    private NotificationMessageId(NotificationSeverity severity,
                                  String summary) {
        this.severity = severity;
        this.summary = summary;
    }
}
