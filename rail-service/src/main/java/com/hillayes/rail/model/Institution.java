package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Institution {
    @EqualsAndHashCode.Include
    public String id;

    @ToString.Include
    public String name;

    @ToString.Include
    public String bic;

    @JsonProperty("transaction_total_days")
    public int transactionTotalDays;

    public List<String> countries;

    public String logo;

    public boolean paymentsEnabled;
}
