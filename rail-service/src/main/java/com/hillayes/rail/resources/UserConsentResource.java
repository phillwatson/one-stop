package com.hillayes.rail.resources;

import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.services.UserConsentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@Path("/api/v1/consents")
@Produces("application/json")
@Consumes("application/json")
@Slf4j
public class UserConsentResource {
    @Inject
    UserConsentService userConsentService;

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
