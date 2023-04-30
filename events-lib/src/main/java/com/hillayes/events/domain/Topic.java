package com.hillayes.events.domain;

/**
 * Enumerates all topics on which events are passed.
 */
public enum Topic {
    USER("User entity related events."),
    USER_AUTH("User authentication related events."),
    CONSENT("Rail consent related events."),
    RETRY_TOPIC("The queue for failed events to be retried.");

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
