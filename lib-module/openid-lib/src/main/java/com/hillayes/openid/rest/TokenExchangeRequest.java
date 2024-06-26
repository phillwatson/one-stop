package com.hillayes.openid.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@RegisterForReflection
public class TokenExchangeRequest {
    @JsonProperty("code")
    public String code;

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("client_secret")
    public String clientSecret;

    @JsonProperty("redirect_uri")
    public String redirectUri;

    @JsonProperty("grant_type")
    public String grantType;

    @JsonProperty("scope")
    public String scope;
}
