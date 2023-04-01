package com.hillayes.user.resource;

import com.hillayes.auth.xsrf.XsrfGenerator;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.model.LoginRequest;
import com.hillayes.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
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

    private final AuthService authService;

    private final XsrfGenerator xsrfGenerator;

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

        return cookieResponse(
            Response.noContent(),
            buildCookies(tokens, accessDuration, refreshDuration))
            .build();
    }

    @GET
    @Path("validate/{auth-provider}")
    public Response oauthLogin(@PathParam("auth-provider") String authProvider,
                               @QueryParam("code") String code,
                               @QueryParam("state") String state,
                               @QueryParam("scope") String scope) {
        log.info("OAuth login [scope: {}, state: {}]", scope, state);
        String[] tokens = authService.oauthLogin(AuthProvider.id(authProvider), code, state, scope);

        return cookieResponse(
            Response.temporaryRedirect(URI.create("http://localhost:3000/accounts")),
            buildCookies(tokens, accessDuration, refreshDuration))
            .build();
    }

    @GET
    @Path("refresh")
    public Response refresh(@Context HttpHeaders headers) {
        log.info("Auth user refresh initiated");
        Cookie refreshTokenCookie = headers.getCookies().get(refreshCookieName);
        if (refreshTokenCookie == null) {
            log.info("No refresh token cookie found.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String[] tokens = authService.refresh(refreshTokenCookie.getValue());

        return cookieResponse(
            Response.noContent(),
            buildCookies(tokens, accessDuration, refreshDuration))
            .build();
    }

    @GET
    @Path("logout")
    public Response logout() {
        log.info("Auth user logout");
        return Response.noContent()
            .cookie(buildCookies(new String[]{null, null}, 0, 0))
            .build();
    }

    private NewCookie[] buildCookies(String[] tokens, long accessTTL, long refreshTTL) {
        NewCookie accessToken = new NewCookie(accessCookieName, tokens[0],
            "/api", null, NewCookie.DEFAULT_VERSION, null,
            (int) accessTTL, Date.from(Instant.now().plusSeconds(accessTTL)),
            false, true);

        NewCookie refreshToken = new NewCookie(refreshCookieName, tokens[1],
            "/api/v1/auth/refresh", null, NewCookie.DEFAULT_VERSION, null,
            (int) refreshTTL, Date.from(Instant.now().plusSeconds(refreshTTL)),
            false, true);

        NewCookie xsrfCookie = xsrfGenerator.generateCookie();

        return new NewCookie[]{accessToken, refreshToken, xsrfCookie};
    }

    /**
     * Set cookies in the response, adding the SameSite=Strict attribute to each cookie.
     *
     * @param responseBuilder the response builder to which the cookies are to be added.
     * @param cookies         the cookies to be added.
     * @return the given response builder with the cookies added.
     */
    public Response.ResponseBuilder cookieResponse(Response.ResponseBuilder responseBuilder,
                                                   NewCookie... cookies) {
        for (NewCookie cookie : cookies) {
            responseBuilder.header("Set-Cookie", cookie + ";SameSite=Strict");
        }
        return responseBuilder;
    }
}
