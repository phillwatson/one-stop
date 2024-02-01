package com.hillayes.notification.domain;

import com.hillayes.events.domain.Topic;

public enum NotificationId {
    CONSENT_DENIED(Topic.CONSENT, NotificationSeverity.info),
    CONSENT_SUSPENDED(Topic.CONSENT, NotificationSeverity.warn),
    CONSENT_EXPIRED(Topic.CONSENT, NotificationSeverity.warn),
    ACCOUNT_ACTIVITY(Topic.USER, NotificationSeverity.warn);

    private final Topic topic;
    private final NotificationSeverity severity;

    private NotificationId(Topic topic, NotificationSeverity severity) {
        this.topic = topic;
        this.severity = severity;
    }

    public Topic getTopic() {
        return topic;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }
}
