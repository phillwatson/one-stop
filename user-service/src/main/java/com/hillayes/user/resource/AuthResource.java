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
import java.util.Map;

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

    private final JsonWebToken jwt;
    private final AuthService authService;

    @POST
    @Path("login")
    public Response login(LoginRequest loginRequest) {
        log.info("Auth user login");
        String[] tokens = authService.login(loginRequest.getUsername(), loginRequest.getPassword().toCharArray());

        return buildCookies(tokens);
    }

    @GET
    @Path("refresh")
    public Response refresh(@Context HttpHeaders headers) {
        log.info("Auth user login");
        Map<String, Cookie> cookies = headers.getCookies();
        Cookie refreshTokenCookie = cookies.get(refreshCookieName);

        String[] tokens = authService.refresh(refreshTokenCookie.getValue());
        return buildCookies(tokens);
    }

    private Response buildCookies(String[] tokens) {
        NewCookie accessToken = new NewCookie(accessCookieName, tokens[0]);
        NewCookie refreshToken = new NewCookie(refreshCookieName, tokens[1]);

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
    @Path("roles-allowed")
    @RolesAllowed({"user"})
    public Response roleAllowed(@Context SecurityContext ctx) {
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
