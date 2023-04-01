package com.hillayes.openid.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenIdConfigResponse {
    @JsonProperty("issuer")
    public String issuer;

    @JsonProperty("authorization_endpoint")
    public String authorizationEndpoint;

    @JsonProperty("jwks_uri")
    public String jwksUri;

    @JsonProperty("token_endpoint")
    public String tokenEndpoint;
}