package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class EndUserAgreementAccepted {
    @JsonProperty("user_agent")
    public String userAgent;

    @JsonPropertyDescription("ip_address")
    public String ipAddress;
}
