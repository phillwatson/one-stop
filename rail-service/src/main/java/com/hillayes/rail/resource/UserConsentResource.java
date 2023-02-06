package com.hillayes.rail.resource;

import com.hillayes.rail.model.UserConsentRequest;
import com.hillayes.rail.services.UserConsentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.UUID;

@Path("/api/v1/consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentResource {
    private final UserConsentService userConsentService;
    private final JsonWebToken jwt;

    @POST
    @RolesAllowed({"user"})
    public Response register(@Context SecurityContext ctx,
                             UserConsentRequest request) {
        String principal = ctx.getUserPrincipal().getName();
        log.info("Registering user's bank [userId: {}, institutionId: {}]", principal, request.getInstitutionId());

        UUID userId = UUID.fromString(principal);
        URI consentLink = userConsentService.register(userId, request.getInstitutionId());

        log.info("Redirecting user to bank consent [link: {}]", consentLink.toASCIIString());
        return Response.seeOther(consentLink).build();
    }

    @GET
    @Path("/accepted")
    @PermitAll
    public Response accepted(@QueryParam("ref") String userConsentId) {
        log.info("User consent accepted [userConsentId: {}]", userConsentId);
        userConsentService.consentAccepted(UUID.fromString(userConsentId));
        return Response.temporaryRedirect(URI.create("/")).build();
    }
}
