package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public class Balance {
    @JsonProperty("balanceAmount")
    public CurrencyAmount balanceAmount;

    @JsonProperty("balanceType")
    public String balanceType;

    @JsonProperty("creditLimitIncluded")
    public Balance creditLimitIncluded;

    @JsonProperty("referenceDate")
    public LocalDate referenceDate;

    @JsonProperty("lastCommittedTransaction")
    public String lastCommittedTransaction;
}
