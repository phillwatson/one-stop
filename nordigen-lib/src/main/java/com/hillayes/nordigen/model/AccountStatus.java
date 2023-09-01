package com.hillayes.nordigen.model;

public enum AccountStatus {
    DISCOVERED("User has successfully authenticated and account is discovered"),
    PROCESSING("Account is being processed by the Institution"),
    ERROR("An error was encountered when processing account"),
    EXPIRED("Access to account has expired as set in End User Agreement"),
    READY("Account has been successfully processed"),
    SUSPENDED("Account has been suspended (more than 10 consecutive failed attempts to access the account");

    private String value;

    AccountStatus(String value) {
        this.value = value;
    }
}
