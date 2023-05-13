package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EndUserAgreement implements Comparable<EndUserAgreement>{
    @JsonProperty("id")
    @EqualsAndHashCode.Include
    public String id;

    @JsonProperty("created")
    public OffsetDateTime created;

    @JsonProperty("max_historical_days")
    public Integer maxHistoricalDays = 90;

    @JsonProperty("access_valid_for_days")
    public Integer accessValidForDays = 90;

    @JsonProperty("access_scope")
    public List<String> accessScope;

    @JsonProperty("accepted")
    public OffsetDateTime accepted;

    @JsonProperty("institution_id")
    public String institutionId;

    public int compareTo(EndUserAgreement other) {
        return (other == null) ? 1 : this.created.compareTo(other.created);
    }
}
