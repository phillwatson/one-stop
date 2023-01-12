package com.hillayes.rail.services.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Account {
    @JsonProperty("id")
    public UUID id;

    @JsonProperty("created")
    public OffsetDateTime created;

    @JsonProperty("last_accessed")
    public OffsetDateTime lastAccessed;

    @JsonProperty("iban")
    public String iban;

    @JsonProperty("institution_id")
    public String institutionId;

    @JsonProperty("status")
    public AccountStatus status;

    @JsonProperty("owner_name")
    public String ownerName;
}
