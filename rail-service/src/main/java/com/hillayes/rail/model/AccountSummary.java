package com.hillayes.rail.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccountSummary {
    @JsonProperty("id")
    @EqualsAndHashCode.Include
    public String id;

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
