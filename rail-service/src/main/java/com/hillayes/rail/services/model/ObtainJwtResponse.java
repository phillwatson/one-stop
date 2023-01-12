package com.hillayes.rail.services.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObtainJwtResponse {
    public String access;
    @JsonProperty("access_expires")
    public Integer accessExpires;
    public String refresh;

    @JsonProperty("refresh_expires")
    public Integer refreshExpires;
}
