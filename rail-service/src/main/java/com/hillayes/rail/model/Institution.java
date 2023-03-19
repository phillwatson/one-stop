package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class Institution {
    @EqualsAndHashCode.Include
    public String id;

    public String name;

    public String bic;

    @JsonProperty("transaction_total_days")
    public int transactionTotalDays;

    public List<String> countries;

    public String logo;

    public boolean paymentsEnabled;
}
