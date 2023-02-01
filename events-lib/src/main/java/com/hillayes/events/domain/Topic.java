package com.hillayes.events.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public enum Topic {
    USER_CREATED("User profile has been created, but not on-boarded."),
    USER_ONBOARDED("User has accepted invitation and has been on-boarded."),
    USER_DECLINED("User has declined invitation."),
    USER_UPDATED("User profile has been updated."),
    USER_DELETED("User profile has been deleted."),
    USER_LOGIN("User profile has logged in."),
    LOGIN_FAILED("User logged in failed."),

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
