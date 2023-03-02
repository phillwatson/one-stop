package com.hillayes.rail.resource;

import com.hillayes.rail.domain.UserConsent;
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
import java.util.List;
import java.util.UUID;

@Path("/api/v1/rails/consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentResource {
    private final UserConsentService userConsentService;
    private final JsonWebToken jwt;

    @GET
    @RolesAllowed("user")
    public Response getConsents(@Context SecurityContext ctx) {
        UUID userId = getUserId(ctx);
        log.info("Listing user's bank [userId: {}]", userId);

        List<UserConsent> result = userConsentService.listConsents(userId);

        log.debug("Listing user's banks [userId: {}, size: {}]", userId, result.size());
        return Response.ok(result).build();
    }

    @POST
    @RolesAllowed("user")
    public Response register(@Context SecurityContext ctx,
                             UserConsentRequest request) {
        UUID userId = getUserId(ctx);
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, request.getInstitutionId());

        URI consentLink = userConsentService.register(userId, request.getInstitutionId());

        log.debug("Redirecting user to bank consent [link: {}]", consentLink.toASCIIString());
        return Response.seeOther(consentLink).build();
    }

    @GET
    @Path("/response")
    @PermitAll
    public Response accepted(@QueryParam("ref") String userConsentId,
                             @QueryParam("error") String error,
                             @QueryParam("details") String details) {
        // A typical consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response
        // ?ref=cbaee100-3f1f-4d7c-9b3b-07244e6a019f
        // &error=UserCancelledSession
        // &details=User+cancelled+the+session.

        log.info("User consent accepted [userConsentId: {}, error: {}, details: {}]", userConsentId, error, details);
        UUID consentId = UUID.fromString(userConsentId);
        if ((error == null) || (error.isBlank())) {
            userConsentService.consentAccepted(consentId);
        } else {
            userConsentService.consentDenied(consentId, error, details);
        }
        return Response.temporaryRedirect(URI.create("/")).build();
    }

    private UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }
}
