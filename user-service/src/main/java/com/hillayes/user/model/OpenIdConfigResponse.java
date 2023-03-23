package com.hillayes.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenIdConfigResponse {
    @JsonProperty("issuer")
    public String issuer;

    @JsonProperty("authorization_endpoint")
    public String authorizationEndpoint;

    @JsonProperty("jwks_uri")
    public String jwksUri;

    @JsonProperty("id_token")
    public String idToken;

    @JsonProperty("scope")
    public String scope;

    @JsonProperty("token_type")
    public String tokenType;
}
