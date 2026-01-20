package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class AccountConsent {
    @JsonProperty("reconfirmed")
    public Instant reconfirmed;

    @JsonProperty("rejected")
    public Instant rejected;

}
