package com.hillayes.openid.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A REST client to call the OpenID auth-provider's token-endpoint, the URI of which
 * is derived from their "well-known" configuration properties.
 * <p>
 * For example; Google's well-known configuration gives the token-endpoint URI of
 * https://oauth2.googleapis.com/token.
 * <p>
 * This REST client is instantiates by the OpenIdFactory.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OpenIdTokenApi {
    /**
     * Calls the auth-provider's token-endpoint to exchange an auth-token for the
     * access and refresh tokens. The response will also include an ID-Token, from
     * which the user's details can be read.
     *
     * @param request the request containing the auth-token to be verified and exchanged.
     * @return the access, refresh and ID tokens exchanged for the given auth-token.
     */
    @POST
    public TokenExchangeResponse exchangeToken(TokenExchangeRequest request);
}
