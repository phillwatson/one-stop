package com.hillayes.user.resource;

import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.auth.xsrf.XsrfRequired;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.onestop.api.LoginRequest;
import com.hillayes.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;

@Path("/api/v1/auth")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AuthResource {
    private final AuthService authService;
    private final AuthTokens authTokens;

    @GET
    @Path("jwks.json")
    public Response getJwkSet() {
        log.info("Retrieving JWK-Set");
        String jwkSet = authService.getJwkSet();
        return Response.ok(jwkSet).build();
    }

    @POST
    @Path("login")
    public Response login(LoginRequest loginRequest,
                          @HeaderParam("user-agent") String userAgent) {
        log.info("Auth user login initiated");
        User user = authService.login(loginRequest.getUsername(), loginRequest.getPassword().toCharArray());

        return authTokens.authResponse(Response.noContent(), user.getId(), user.getRoles());
    }

    /**
     * The call-back URI for open-id authentication.
     *
     * @param uriInfo the platform URI context from which redirections can be constructed.
     * @param authProvider the identity of the OpenID auth provider implementation.
     * @param code the OpenID provider's auth code to be exchanged for their access tokens.
     * @param state the state information passed in the initial request to the auth provider.
     * @param scope the scope of access to the authenticated user's data.
     * @param error if auth failed, this is the error code for that failure.
     * @param errorUri if auth failed, this is the URI for more info about that failure.
     * @return the redirection response.
     */
    @GET
    @Path("validate/{auth-provider}")
    public Response oauthLogin(@Context UriInfo uriInfo,
                               @PathParam("auth-provider") String authProvider,
                               @QueryParam("code") String code,
                               @QueryParam("state") String state,
                               @QueryParam("scope") String scope,
                               @QueryParam("error") String error,
                               @QueryParam("error_uri") String errorUri) {
        log.info("OAuth login [scope: {}, state: {}, error: {}]", scope, state, error);

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        if (error != null) {
            URI redirect = uriBuilder.queryParam("error", error).build();
            log.debug("OAuth login fail [redirect: {}, errorUri: {}]", redirect, errorUri);
            return authTokens.deleteCookies(Response.temporaryRedirect(redirect));
        }

        User user = authService.oauthLogin(AuthProvider.id(authProvider), code, state, scope);

        URI redirect = uriBuilder.build();
        log.debug("OAuth login success [redirect: {}]", redirect);
        return authTokens.authResponse(Response.temporaryRedirect(redirect), user.getId(), user.getRoles());
    }

    @GET
    @Path("refresh")
    @XsrfRequired
    public Response refresh(@Context HttpHeaders headers) {
        log.info("Auth user refresh initiated");
        JsonWebToken refreshToken = authTokens.getRefreshToken(headers.getCookies())
            .orElse(null);

        if (refreshToken == null) {
            log.info("No refresh token cookie found.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        User user = authService.refresh(refreshToken);
        return authTokens.authResponse(Response.noContent(), user.getId(), user.getRoles());
    }

    @GET
    @Path("logout")
    public Response logout() {
        log.info("Auth user logout");
        return authTokens.deleteCookies(Response.noContent());
    }
}
