package com.hillayes.nordigen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RefreshJwtResponse {
    public String access;
    @JsonProperty("access_expires")
    public Integer accessExpires;
}
