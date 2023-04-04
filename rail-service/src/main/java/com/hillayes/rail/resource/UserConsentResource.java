package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.*;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.service.RailAccountService;
import com.hillayes.rail.service.RequisitionService;
import com.hillayes.rail.service.UserConsentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

@Path("/api/v1/rails/consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentResource {
    private final UserConsentService userConsentService;
    private final InstitutionService institutionService;
    private final RequisitionService requisitionService;
    private final RailAccountService railAccountService;
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

    @GET
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response getConsentForInstitution(@Context SecurityContext ctx,
                                             @PathParam("institutionId") String institutionId) {
        UUID userId = getUserId(ctx);
        log.info("Getting user's consent record [userId: {}, institutionId: {}]", userId, institutionId);

        UserConsent consent = userConsentService.getUserConsent(userId, institutionId)
            .orElseThrow(() -> new NotFoundException("UserConsent", Map.of("userId", userId, "institutionId", institutionId)));

        Institution institution = institutionService.get(consent.getInstitutionId());
        List<AccountDetail> accountDetails = requisitionService.get(consent.getRequisitionId())
            .map(requisition -> requisition.accounts)
            .orElse(Collections.emptyList())
            .stream()
            .map(railAccountService::get)
            .toList();

        UserConsentResponse result = UserConsentResponse.builder()
            .id(consent.getId())
            .institutionId(consent.getInstitutionId())
            .institutionName(institution.name)
            .dateGiven(consent.getDateGiven())
            .agreementExpires(consent.getAgreementExpires())
            .maxHistory(consent.getMaxHistory())
            .status(consent.getStatus())
            .accounts(accountDetails)
            .build();

        log.info("Getting user's consent record [userId: {}, institutionId: {}, consentId: {}]",
            userId, institutionId, consent.getId());
        return Response.ok(result).build();
    }

    @DELETE
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response deleteConsent(@Context SecurityContext ctx,
                                  @PathParam("institutionId") String institutionId) {
        UUID userId = getUserId(ctx);
        log.info("Deleting user's consent record [userId: {}, institutionId: {}]", userId, institutionId);

        UserConsent result = userConsentService.getUserConsent(userId, institutionId)
            .orElseThrow(() -> new NotFoundException("UserConsent", Map.of("userId", userId, "institutionId", institutionId)));

        userConsentService.consentCancelled(result.getId());
        return Response.noContent().build();
    }

    @POST
    @RolesAllowed("user")
    public Response register(@Context SecurityContext ctx,
                             UserConsentRequest request) {
        UUID userId = getUserId(ctx);
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, request.getInstitutionId());

        URI consentLink = userConsentService.register(userId, request.getInstitutionId());

        log.debug("Redirecting user to bank consent [link: {}]", consentLink.toASCIIString());
        return Response.ok(consentLink).build();
    }

    @GET
    @Path("/response")
    @PermitAll
    public Response consentResponse(@QueryParam("ref") String userConsentId,
                                    @QueryParam("error") String error,
                                    @QueryParam("details") String details) {
        // A typical consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response
        // ?ref=cbaee100-3f1f-4d7c-9b3b-07244e6a019f
        // &error=UserCancelledSession
        // &details=User+cancelled+the+session.

        log.info("User consent response [userConsentId: {}, error: {}, details: {}]", userConsentId, error, details);
        UUID consentId = UUID.fromString(userConsentId);
        if ((error == null) || (error.isBlank())) {
            userConsentService.consentGiven(consentId);
        } else {
            userConsentService.consentDenied(consentId, error, details);
        }
        return Response.temporaryRedirect(URI.create("/")).build();
    }

    private UUID getUserId(SecurityContext context) {
        return UUID.fromString(context.getUserPrincipal().getName());
    }
}
