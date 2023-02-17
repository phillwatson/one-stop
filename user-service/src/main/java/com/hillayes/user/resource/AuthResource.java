package com.hillayes.user.resource;

import com.hillayes.user.model.LoginRequest;
import com.hillayes.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Instant;
import java.util.Date;

@Path("/api/v1/auth")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AuthResource {
    @ConfigProperty(name = "mp.jwt.token.cookie")
    String accessCookieName;

    @ConfigProperty(name = "one-stop.jwt.refresh-token.cookie")
    String refreshCookieName;

    @ConfigProperty(name = "one-stop.jwt.access-token.duration-secs")
    long accessDuration;

    @ConfigProperty(name = "one-stop.jwt.refresh-token.duration-secs")
    long refreshDuration;

    private final JsonWebToken jwt;
    private final AuthService authService;

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
        String[] tokens = authService.login(loginRequest.getUsername(), loginRequest.getPassword().toCharArray());

        return buildCookies(tokens, accessDuration, refreshDuration);
    }

    @GET
    @Path("refresh")
    public Response refresh(@Context HttpHeaders headers) {
        log.info("Auth user refresh initiated");
        Cookie refreshTokenCookie = headers.getCookies().get(refreshCookieName);
        if (refreshTokenCookie == null) {
            log.info("No refresh token cookie found.");
            throw new NotAuthorizedException("refresh-token");
        }

        String[] tokens = authService.refresh(refreshTokenCookie.getValue());

        return buildCookies(tokens, accessDuration, refreshDuration);
    }

    @GET
    @Path("logout")
    public Response logout() {
        log.info("Auth user logout initiated");
        return buildCookies(new String[] {"",""}, 0, 0);
    }

    private Response buildCookies(String[] tokens, long accessTTL, long refreshTTL) {
        NewCookie accessToken = new NewCookie(accessCookieName, tokens[0],
            "/api","localhost", NewCookie.DEFAULT_VERSION,null,
            (int)accessTTL, Date.from(Instant.now().plusSeconds(accessTTL)),
            false, true);

        NewCookie refreshToken = new NewCookie(refreshCookieName, tokens[1],
            "/api","localhost", NewCookie.DEFAULT_VERSION,null,
            (int)refreshTTL, Date.from(Instant.now().plusSeconds(refreshTTL)),
            false, true);

        return Response.noContent()
            .cookie(accessToken, refreshToken)
            .build();
    }
}
