package com.hillayes.rail.repository;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@Path("/api/v2/token")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AuthRepository {
    @POST
    @Path("/new/")
    @Consumes("application/x-www-form-urlencoded")
    public ObtainJwtResponse newToken(@FormParam("secret_id") String secretId,
                                      @FormParam("secret_key") String secretKey);

    @POST
    @Path("/refresh/")
    @Consumes("application/x-www-form-urlencoded")
    public RefreshJwtResponse refreshToken(@FormParam("refresh") String refresh);
}
