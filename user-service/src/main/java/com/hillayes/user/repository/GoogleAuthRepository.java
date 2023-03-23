package com.hillayes.user.repository;

import com.hillayes.user.model.TokenExchangeRequest;
import com.hillayes.user.model.TokenExchangeResponse;
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
public interface GoogleAuthRepository {
    @POST
    @Path("token")
    public TokenExchangeResponse exchangeToken(TokenExchangeRequest request);
}
