package com.hillayes.user.resource;

import com.hillayes.user.service.AuthService;
import io.smallrye.jwt.build.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/api/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AuthResource {
    private final JsonWebToken jwt;
    private final AuthService authService;

    @POST
    @Path("login")
    public Response login(@QueryParam("role") String role) {
        String token = authService.login("snoopy", null);
        log.info("Token: {}", token);
        return Response.ok().entity(token).build();
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

    private void logIt(SecurityContext ctx){
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
