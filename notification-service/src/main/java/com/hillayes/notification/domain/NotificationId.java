package com.hillayes.notification.domain;

import com.hillayes.events.domain.Topic;

/**
 * Serves to identify the notification message template within the
 * configuration. It also provides the topic and severity of the
 * notification.
 *
 * @see com.hillayes.notification.config.NotificationConfiguration
 */
public enum NotificationId {
    CONSENT_DENIED(Topic.CONSENT, NotificationSeverity.info),
    CONSENT_SUSPENDED(Topic.CONSENT, NotificationSeverity.warning),
    CONSENT_EXPIRED(Topic.CONSENT, NotificationSeverity.warning),
    CONSENT_TIMEOUT(Topic.CONSENT, NotificationSeverity.info),
    ACCOUNT_ACTIVITY(Topic.USER, NotificationSeverity.warning),
    AUDIT_ISSUE_FOUND(Topic.TRANSACTION_AUDIT, NotificationSeverity.warning),;

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
