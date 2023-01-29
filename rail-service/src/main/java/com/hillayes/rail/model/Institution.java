package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Institution {
    public String id;
    public String name;
    public String bic;

    @JsonProperty("transaction_total_days")
    public int transactionTotalDays;

    public List<String> countries;

    public String logo;
}
