package com.hillayes.events.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public enum Topic {
    USER("User entity related events."),
    USER_AUTH("User authentication related events."),
    PAYMENT_AUDIT("Auditable action on a payment."),
    EVENT_HOSPITAL("The dead-letter queue for failed events.");

    private String summary;

    private Topic(String summary) {
        this.summary = summary;
    }

    public String topicName() {
        return name().toLowerCase();
    }

    public String getSummary() {
        return summary;
    }

    public static Collection<String> allTopicNames() {
        return Arrays.stream(Topic.values()).map(Topic::topicName).collect(Collectors.toList());
    }
}
