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

    /**
     * The date & time at which the requisition was created.
     */
    @JsonProperty("created")
    public OffsetDateTime created;

    @JsonProperty("status")
    public RequisitionStatus status;

    /**
     * array of account IDs retrieved within a scope of this requisition
     */
    @JsonProperty("accounts")
    public List<String> accounts;

    /**
     * link to initiate authorization with Institution
     */
    @JsonProperty("link")
    public String link;

    /**
     * redirect URL to your application after end-user authorization with ASPSP
     */
    @JsonProperty("redirect")
    public String redirect;

    /**
     * an Institution ID for this Requisition
     */
    @JsonProperty("institution_id")
    public String institutionId;

    /**
     * The EUA associated with this requisition
     */
    @JsonProperty("agreement")
    public String agreement;

    /**
     * additional ID to identify the end user
     */
    @JsonProperty("reference")
    public String reference;

    /**
     * A two-letter country code (ISO 639-1)
     */
    @JsonProperty("user_language")
    public String userLanguage;

    /**
     * optional SSN field to verify ownership of the account
     */
    @JsonProperty("ssn")
    public String ssn;

    /**
     * option to enable account selection view for the end user
     */
    @JsonProperty("account_selection")
    public Boolean accountSelection;

    /**
     * enable redirect back to the client after account list received
     */
    @JsonProperty("redirect_immediate")
    public Boolean redirectImmediate;

    public int compareTo(Requisition other) {
        return (other == null) ? 1 : this.created.compareTo(other.created);
    }
}
