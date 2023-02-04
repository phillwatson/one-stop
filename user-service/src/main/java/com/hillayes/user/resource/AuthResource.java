package com.hillayes.user.resource;

import com.hillayes.user.model.LoginRequest;
import com.hillayes.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Instant;
import java.util.Date;

@Path("/api/v1/auth")
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

    @POST
    @Path("login")
    @PermitAll
    public Response login(LoginRequest loginRequest) {
        log.info("Auth user login initiated");
        String[] tokens = authService.login(loginRequest.getUsername(), loginRequest.getPassword().toCharArray());

        return buildCookies(tokens, accessDuration, refreshDuration);
    }

    @GET
    @Path("refresh")
    @PermitAll
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
    @PermitAll
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

    @GET
    @Path("permit-all")
    @PermitAll
    public Response permitAll(@Context SecurityContext ctx) {
        logIt(ctx);
        return Response.noContent().build();
    }

    @GET
    @Path("user-allowed")
    @RolesAllowed({"user"})
    public Response userAllowed(@Context SecurityContext ctx) {
        logIt(ctx);
        return Response.noContent().build();
    }

    @GET
    @Path("admin-allowed")
    @RolesAllowed({"admin"})
    public Response adminAllowed(@Context SecurityContext ctx) {
        logIt(ctx);
        return Response.noContent().build();
    }

    private void logIt(SecurityContext ctx) {
        String name;
        if (ctx.getUserPrincipal() == null) {
            name = "anonymous";
        } else if (!ctx.getUserPrincipal().getName().equals(jwt.getName())) {
            throw new InternalServerErrorException("Principal and JsonWebToken names do not match");
        } else {
            name = ctx.getUserPrincipal().getName();
        }

        log.info("hello {}, isHttps: {}, authScheme: {}, claims: {}",
            name, ctx.isSecure(), ctx.getAuthenticationScheme(), jwt.getClaimNames());
    }
}
