package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class AccountBalance {
    @JsonProperty("balanceAmount")
    public CurrencyAmount balanceAmount;

    @JsonProperty("balanceType")
    public String balanceType;

    @JsonProperty("referenceDate")
    public LocalDate referenceDate;
}
