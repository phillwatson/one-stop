package com.hillayes.user.openid.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenExchangeResponse {
    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("expiresIn")
    public String expiresIn;

    @JsonProperty("id_token")
    public String idToken;

    @JsonProperty("scope")
    public String scope;

    @JsonProperty("token_type")
    public String tokenType;
}
