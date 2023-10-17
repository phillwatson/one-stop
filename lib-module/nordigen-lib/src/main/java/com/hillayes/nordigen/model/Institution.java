package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Institution implements Comparable<Institution> {
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

    @Override
    public int compareTo(Institution other) {
        if (other == this) {
            return 0;
        }
        if (other == null) {
            return 1;
        }

        return this.name.compareTo(other.name);
    }
}
