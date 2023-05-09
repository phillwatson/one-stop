package com.hillayes.email.config;

public enum TemplateName {
    USER_REGISTERED("user-registered"),
    USER_CREATED("user-created"),
    USER_UPDATED("user-updated"),
    USER_DELETED("user-deleted");

    private final String templateName;

    TemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}
