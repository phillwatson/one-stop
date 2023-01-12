package com.hillayes.rail.services;

import com.hillayes.rail.services.model.ObtainJwtResponse;
import com.hillayes.rail.services.model.RefreshJwtResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.*;

@RegisterRestClient(configKey = "nordigen-api")
@Path("/api/v2/token")
@Produces("application/json")
@Consumes("application/json")
@Singleton
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
