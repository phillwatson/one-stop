package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EndUserAgreement implements Comparable<EndUserAgreement>{
    @JsonProperty("id")
    @EqualsAndHashCode.Include
    public String id;

    /**
     * The date & time at which the end user agreement was created.
     */
    @JsonProperty("created")
    public OffsetDateTime created;

    /**
     * an Institution ID for this EUA
     */
    @JsonProperty("institution_id")
    public String institutionId;

    /**
     * Maximum number of days of transaction data to retrieve.
     */
    @JsonProperty("max_historical_days")
    public Integer maxHistoricalDays = 90;

    /**
     * Number of days from acceptance that the access can be used.
     */
    @JsonProperty("access_valid_for_days")
    public Integer accessValidForDays = 90;

    @JsonProperty("access_scope")
    public List<String> accessScope;

    /**
     * The date & time at which the end user accepted the agreement.
     */
    @JsonProperty("accepted")
    public OffsetDateTime accepted;

    /**
     * if this agreement can be extended. Supported by GB banks only.
     */
    @JsonProperty("reconfirmation")
    public boolean reconfirmation;

    public int compareTo(EndUserAgreement other) {
        return (other == null) ? 1 : this.created.compareTo(other.created);
    }
}
