package com.hillayes.events.events.auth;

public enum SuspiciousActivity {
    EMAIL_REGISTRATION("Your email address was used to register an account.");

    private final String message;

    SuspiciousActivity(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
