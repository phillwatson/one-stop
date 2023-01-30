package com.hillayes.rail.resource;

import com.hillayes.rail.services.UserConsentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@Path("/api/v1/consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentResource {
    private final UserConsentService userConsentService;

    @POST
    public Response register(String institutionId) {
        log.info("Registering user's bank [userId: {}, institutionId: {}]", null, institutionId);
        URI consentLink = userConsentService.register(null, institutionId);
        log.info("Redirecting user to bank consent [link: {}]", consentLink.toASCIIString());
        return Response.temporaryRedirect(consentLink).build();
    }

    @GET
    @Path("/accepted")
    public Response accepted(@QueryParam("ref") String userConsentId) {
        log.info("User consent accepted [userConsentId: {}]", userConsentId);
        userConsentService.consentAccepted(UUID.fromString(userConsentId));
        return Response.temporaryRedirect(URI.create("/")).build();
    }
}
