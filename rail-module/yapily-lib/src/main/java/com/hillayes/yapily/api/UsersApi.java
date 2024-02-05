package com.hillayes.yapily.api;

import com.hillayes.yapily.model.ApiResponseOfUserDeleteResponse;
import com.hillayes.yapily.model.ApplicationUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BasicHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface UsersApi {
    /**
     * Get the user information using the user Id
     *
     * @param userId the ID of the user (as assigned by the rail) to be retrieved.
     * @return the identified user information.
     */
    @GET
    @Path("/users/{userId}")
    public ApplicationUser getUser(@PathParam("userId") UUID userId);

    /**
     * Delete the identified user.
     * @param userId the ID of the user (as assigned by the rail) to be deleted.
     * @return the identified user information.
     */
    @DELETE
    @Path("/users/{userId}")
    public ApiResponseOfUserDeleteResponse deleteUser(@PathParam("userId") UUID userId);
}
