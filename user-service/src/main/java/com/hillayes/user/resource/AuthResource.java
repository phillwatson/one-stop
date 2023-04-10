package com.hillayes.user.resource;

import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.auth.xsrf.XsrfRequired;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.User;
import com.hillayes.user.model.LoginRequest;
import com.hillayes.user.service.AuthService;
import io.smallrye.jwt.auth.principal.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public Response login(LoginRequest loginRequest) {
        log.info("Auth user login initiated");
        User user = authService.login(loginRequest.getUsername(), loginRequest.getPassword().toCharArray());

        return authTokens.authResponse(Response.noContent(), user.getId(), user.getRoles());
    }

    @GET
    @Path("validate/{auth-provider}")
    public Response oauthLogin(@Context HttpHeaders headers,
                               @PathParam("auth-provider") String authProvider,
                               @QueryParam("code") String code,
                               @QueryParam("state") String state,
                               @QueryParam("scope") String scope,
                               @QueryParam("error") String error,
                               @QueryParam("error_uri") String errorUri) {
        log.info("OAuth login [scope: {}, state: {}, error: {}]", scope, state, error);

        StringBuilder redirectUri = new StringBuilder()
            .append(headers.getHeaderString("x-forwarded-proto")).append("://")
            .append(headers.getHeaderString("x-forwarded-host"));

        if (error != null) {
            URI redirect = URI.create(redirectUri.append("/sign-in?error=").append(error).toString());
            return authTokens.deleteCookies(Response.temporaryRedirect(redirect));
        }

        User user = authService.oauthLogin(AuthProvider.id(authProvider), code, state, scope);

        URI redirect = URI.create(redirectUri.append("/accounts").toString());
        return authTokens.authResponse( Response.temporaryRedirect(redirect), user.getId(), user.getRoles());
    }

    @GET
    @Path("refresh")
    @XsrfRequired
    public Response refresh(@Context HttpHeaders headers) {
        log.info("Auth user refresh initiated");
        try {
            JsonWebToken refreshToken = authTokens.getRefreshToken(headers.getCookies())
                .orElse(null);

            if (refreshToken == null) {
                log.info("No refresh token cookie found.");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            User user = authService.refresh(refreshToken);
            return authTokens.authResponse(
                Response.noContent(), user.getId(), user.getRoles());
        } catch (ParseException e) {
            log.error("Failed to verify refresh token.", e);
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("logout")
    public Response logout() {
        log.info("Auth user logout");
        return authTokens.deleteCookies(Response.noContent());
    }
}
