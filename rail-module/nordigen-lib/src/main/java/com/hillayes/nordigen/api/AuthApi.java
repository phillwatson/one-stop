package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.ObtainJwtResponse;
import com.hillayes.nordigen.model.RefreshJwtResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@Path("/api/v2/token")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public interface AuthApi {
    @POST
    @Path("/new/")
    public ObtainJwtResponse newToken(@FormParam("secret_id") String secretId,
                                      @FormParam("secret_key") String secretKey);

    @POST
    @Path("/refresh/")
    public RefreshJwtResponse refreshToken(@FormParam("refresh") String refresh);
}
