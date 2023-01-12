package com.hillayes.rail.services.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Requisition {
    public UUID id;

    @JsonProperty("created")
    public OffsetDateTime created;

    @JsonProperty("status")
    public RequisitionStatus status;

    @JsonProperty("accounts")
    public List<UUID> accounts;

    @JsonProperty("link")
    public String link;

    @JsonProperty("redirect")
    public String redirect;

    @JsonProperty("institution_id")
    public String institutionId;

    @JsonProperty("agreement")
    public UUID agreement;

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
}
