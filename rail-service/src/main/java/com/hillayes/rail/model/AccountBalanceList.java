package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AccountBalanceList {
    @JsonProperty("balances")
    public List<Balance> balances;
}
