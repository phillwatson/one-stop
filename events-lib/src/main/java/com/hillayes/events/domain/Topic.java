package com.hillayes.events.domain;

/**
 * Enumerates all topics on which events are passed.
 */
public enum Topic {
    USER("User entity related events."),
    USER_AUTH("User authentication related events."),
    CONSENT("Rail consent related events."),
    EVENT_HOSPITAL("The dead-letter queue for failed events.");

    private final String summary;

    Topic(String summary) {
        this.summary = summary;
    }

    public String topicName() {
        return name().toLowerCase();
    }

    public String getSummary() {
        return summary;
    }
}
