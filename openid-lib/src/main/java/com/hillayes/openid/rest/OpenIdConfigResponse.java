package com.hillayes.openid.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the response from the Auth-Providers "well-known" configuration URI.
 * These properties are then used to interact with the Auth-Provider and verify the
 * tokens it tokens.
 */
public class OpenIdConfigResponse {
    /**
     * Identifies the issuer of the JSON Web Tokens used for authorisation. This will
     * be used as part of the ID-Token verification.
     */
    @JsonProperty("issuer")
    public String issuer;

    /**
     * The URI used to initiate an Open-ID Connect authorisation flow. Whilst the auth
     * flow can be initiated on the back-end, we have chosen to initiate it on the
     * front-end.
     */
    @JsonProperty("authorization_endpoint")
    public String authorizationEndpoint;

    /**
     * The URI on which the JSON Web Key Set can be found. These public keys are used
     * to verify the signature used to sign the JWT ID-token returned from the auth-code
     * exchange.
     */
    @JsonProperty("jwks_uri")
    public String jwksUri;

    /**
     * The URI on which an Open-ID auth-token can be exchanged for the access-token,
     * refresh-token and ID-token.
     */
    @JsonProperty("token_endpoint")
    public String tokenEndpoint;
}
