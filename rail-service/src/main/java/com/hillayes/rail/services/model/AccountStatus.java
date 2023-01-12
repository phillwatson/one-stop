package com.hillayes.rail.services.model;

public enum AccountStatus {
    DISCOVERED_USER_HAS_SUCCESSFULLY_AUTHENTICATED_AND_ACCOUNT_IS_DISCOVERED_("{\"DISCOVERED\":\"User has successfully authenticated and account is discovered\"}"),
    PROCESSING_ACCOUNT_IS_BEING_PROCESSED_BY_THE_INSTITUTION_("{\"PROCESSING\":\"Account is being processed by the Institution\"}"),
    ERROR_AN_ERROR_WAS_ENCOUNTERED_WHEN_PROCESSING_ACCOUNT_("{\"ERROR\":\"An error was encountered when processing account\"}"),
    EXPIRED_ACCESS_TO_ACCOUNT_HAS_EXPIRED_AS_SET_IN_END_USER_AGREEMENT_("{\"EXPIRED\":\"Access to account has expired as set in End User Agreement\"}"),
    READY_ACCOUNT_HAS_BEEN_SUCCESSFULLY_PROCESSED_("{\"READY\":\"Account has been successfully processed\"}"),
    SUSPENDED_ACCOUNT_HAS_BEEN_SUSPENDED_MORE_THAN_10_CONSECUTIVE_FAILED_ATTEMPTS_TO_ACCESS_THE_ACCOUNT_("{\"SUSPENDED\":\"Account has been suspended (more than 10 consecutive failed attempts to access the account)\"}");

    private String value;

    AccountStatus(String value) {
        this.value = value;
    }
}
