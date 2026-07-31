package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class BalanceAfterTransaction {
    @JsonProperty("balanceAmount")
    public CurrencyAmount balanceAmount;

    @JsonProperty("balanceType")
    public String balanceType;
}
