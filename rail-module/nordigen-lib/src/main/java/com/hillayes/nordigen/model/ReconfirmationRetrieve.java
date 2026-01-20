package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReconfirmationRetrieve {
    @JsonProperty("created")
    public Instant created;

    /**
     * Reconfirmation URL to be provided to PSU.
     */
    @JsonProperty("reconfirmation_url")
    public String reconfirmationUrl;

    /**
     * Datetime from when PSU will be able to access reconfirmation URL.
     */
    @JsonProperty("url_valid_from")
    public Instant urlValidFrom;

    /**
     * Datetime until when PSU will be able to access reconfirmation URL.
     */
    @JsonProperty("url_valid_to")
    public Instant urlValidTo;

    /**
     * Optional redirect URL for reconfirmation to override requisition's redirect.
     */
    @JsonProperty("redirect")
    public String redirect;

    /**
     * Last time when reconfirmation was accessed (this does not mean that it was accessed by PSU).
     */
    @JsonProperty("last_accessed")
    public Instant lastAccessed;

    /**
     * Last time reconfirmation was submitted (it can be submitted multiple times).
     */
    @JsonProperty("last_submitted")
    public Instant lastSubmitted;

    /**
     * Dictionary of accounts and their reconfirm and reject timestamps
     */
    @JsonProperty("accounts")
    public Map<String, AccountConsent> accounts;
}
