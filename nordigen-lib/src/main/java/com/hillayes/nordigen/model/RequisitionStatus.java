package com.hillayes.nordigen.model;

public enum RequisitionStatus {
    CR, // CREATED Requisition has been successfully created
    GC, // GIVING_CONSENT End-user is giving consent at GoCardless's consent screen
    UA, // UNDERGOING_AUTHENTICATION End-user is redirected to the financial institution for authentication
    RJ, // REJECTED Either SSN verification has failed or end-user has entered incorrect credentials
    SA, // SELECTING_ACCOUNTS End-user is selecting accounts
    GA, // GRANTING_ACCESS End-user is granting access to their account information
    LN, // LINKED Account has been successfully linked to requisition
    SU, // SUSPENDED Requisition is suspended due to numerous consecutive errors that happened while accessing its accounts
    EX, // EXPIRED Access to accounts has expired as set in End User Agreement
    ID,
    ER;

    /**
     * Returns the next status in the stages described in the Nordigen API documentation.
     * If there is no next status, returns null.
     *
     * see https://nordigen.com/en/account_information_documenation/integration/statuses/
     */
    public RequisitionStatus nextStatus() {
        int index = this.ordinal();
        return (index >= EX.ordinal()) ? null : RequisitionStatus.values()[index + 1];
    }
}
