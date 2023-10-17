package com.hillayes.openid.github;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

import java.util.Map;

/**
 * A REST client to call the GitHub's User API. This is used as part of the Open-ID
 * authentication to obtain the authenticated user's details.
 * <p>
 * GitHub does not provide a full Open-ID Connect auth-code flow, and only returns
 * an access-token on authentication. Using the access-token, we need to use this
 * API to obtain more information about the user.
 */
@ClientHeaderParam(name = "X-GitHub-Api-Version", value = "2022-11-28")
@Consumes("application/vnd.github+json")
@Produces(MediaType.APPLICATION_JSON)
public interface GitHubApiClient {
    /**
     * Calls the GitHib REST API to retrieve the profile information of the user
     * identified by the access-token in the Authorization header.
     *
     * @param accessToken the access-token prefixed with "Bearer ".
     * @return the profile information for the user identified by the access-token.
     */
    @GET
    @Path("/user")
    public Map<String,Object> getUserProfile(@HeaderParam("Authorization") String accessToken);
}
