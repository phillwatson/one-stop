package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Requisition implements Comparable<Requisition> {
    @EqualsAndHashCode.Include
    public String id;

    @JsonProperty("created")
    public OffsetDateTime created;

    @JsonProperty("status")
    public RequisitionStatus status;

    @JsonProperty("accounts")
    public List<String> accounts;

    @JsonProperty("link")
    public String link;

    @JsonProperty("redirect")
    public String redirect;

    @JsonProperty("institution_id")
    public String institutionId;

    @JsonProperty("agreement")
    public String agreement;

    @JsonProperty("reference")
    public String reference;

    @JsonProperty("user_language")
    public String userLanguage;

    @JsonProperty("ssn")
    public String ssn;

    @JsonProperty("account_selection")
    public Boolean accountSelection;

    @JsonProperty("redirect_immediate")
    public Boolean redirectImmediate;

    public int compareTo(Requisition other) {
        return (other == null) ? 1 : this.created.compareTo(other.created);
    }
}
