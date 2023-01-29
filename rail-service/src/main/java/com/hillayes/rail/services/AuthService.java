package com.hillayes.rail.services;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@Path("/api/v2/token")
@Produces("application/json")
@Consumes("application/json")
public interface AuthService {
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
