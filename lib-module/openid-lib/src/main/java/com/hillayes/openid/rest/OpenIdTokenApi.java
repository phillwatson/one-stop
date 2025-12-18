package com.hillayes.openid.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

/**
 * A REST client to call the OpenID auth-provider's token-endpoint, the URI of which
 * is derived from their "well-known" configuration properties.
 * <p>
 * For example; Google's well-known configuration gives the token-endpoint URI of
 * https://oauth2.googleapis.com/token.
 * <p>
 * This REST client is instantiated by the OpenIdFactory.
 */
@Produces(MediaType.APPLICATION_JSON)
@RegisterForReflection
//@RegisterForProxy(targets = {
//    OpenIdTokenApi.class,
//    org.jboss.resteasy.client.jaxrs.internal.proxy.ResteasyClientProxy.class,
//    org.jboss.resteasy.microprofile.client.RestClientProxy.class,
//    java.io.Closeable.class
//})
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
    @Consumes(MediaType.APPLICATION_JSON)
    public TokenExchangeResponse exchangeToken(TokenExchangeRequest request);

    /**
     * Calls the auth-provider's token-endpoint to exchange an auth-token for the
     * access and refresh tokens. The response will also include an ID-Token, from
     * which the user's details can be read.
     *
     * @return the access, refresh and ID tokens exchanged for the given auth-token.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TokenExchangeResponse exchangeToken(@FormParam("grant_type") String grantType,
                                               @FormParam("client_id") String clientId,
                                               @FormParam("client_secret") String clientSecret,
                                               @FormParam("code") String authorizationCode,
                                               @FormParam("redirect_uri") String redirectUri);

    /**
     * Calls the auth-provider's token-endpoint to exchange an auth-token for the
     * access and refresh tokens. The response will also include an ID-Token, from
     * which the user's details can be read.
     *
     * @param form the request containing the auth-token to be verified and exchanged.
     * @return the access, refresh and ID tokens exchanged for the given auth-token.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TokenExchangeResponse exchangeToken(Form form);
}
