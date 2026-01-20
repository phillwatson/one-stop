package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class EndUserAgreementAccepted {
    /**
     * user agent string for the end user
     */
    @JsonProperty("user_agent")
    public String userAgent;

    /**
     * end user IP address
     */
    @JsonPropertyDescription("ip_address")
    public String ipAddress;
}
