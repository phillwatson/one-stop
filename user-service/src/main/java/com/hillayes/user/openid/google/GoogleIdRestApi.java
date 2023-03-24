package com.hillayes.user.openid.google;

import com.hillayes.user.openid.rest.TokenExchangeRequest;
import com.hillayes.user.openid.rest.TokenExchangeResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "google-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface GoogleIdRestApi {
    @POST
    @Path("token")
    public TokenExchangeResponse exchangeToken(TokenExchangeRequest request);
}
