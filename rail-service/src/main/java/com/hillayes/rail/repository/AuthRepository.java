package com.hillayes.rail.repository;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@Path("/api/v2/token")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public interface AuthRepository {
    @POST
    @Path("/new/")
    public ObtainJwtResponse newToken(@FormParam("secret_id") String secretId,
                                      @FormParam("secret_key") String secretKey);

    @POST
    @Path("/refresh/")
    public RefreshJwtResponse refreshToken(@FormParam("refresh") String refresh);
}
