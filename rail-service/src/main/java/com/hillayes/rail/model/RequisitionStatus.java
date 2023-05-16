package com.hillayes.rail.model;

public enum RequisitionStatus {
    CR,
    GC,
    UA,
    RJ,
    SA,
    GA,
    LN,
    SU,
    EX,
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
