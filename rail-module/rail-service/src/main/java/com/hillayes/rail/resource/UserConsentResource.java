package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.AccountSummaryResponse;
import com.hillayes.onestop.api.PaginatedUserConsents;
import com.hillayes.onestop.api.UserConsentRequest;
import com.hillayes.onestop.api.UserConsentResponse;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.Institution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.UserConsentService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/rails/consents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentResource {
    private final UserConsentService userConsentService;
    private final RailProviderApi railProviderApi;
    private final AccountService accountService;

    @GET
    @RolesAllowed("user")
    public Response getConsents(@Context SecurityContext ctx,
                                @Context UriInfo uriInfo,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Listing user's banks [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);

        Page<UserConsent> consentsPage = userConsentService.listConsents(userId, page, pageSize);

        PaginatedUserConsents response = new PaginatedUserConsents()
            .page(consentsPage.getPageIndex())
            .pageSize(consentsPage.getPageSize())
            .count(consentsPage.getContentSize())
            .total(consentsPage.getTotalCount())
            .items(consentsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, consentsPage));

        log.debug("Listing user's banks [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            userId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response getConsentForInstitution(@Context SecurityContext ctx,
                                             @PathParam("institutionId") String institutionId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Getting user's consent record [userId: {}, institutionId: {}]", userId, institutionId);

        UserConsent consent = userConsentService.getUserConsent(userId, institutionId)
            .orElseThrow(() -> new NotFoundException("UserConsent", Map.of("userId", userId, "institutionId", institutionId)));

        UserConsentResponse result = marshal(consent);

        log.debug("Getting user's consent record [userId: {}, institutionId: {}, consentId: {}]",
            userId, institutionId, consent.getId());
        return Response.ok(result).build();
    }

    @DELETE
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response deleteConsent(@Context SecurityContext ctx,
                                  @PathParam("institutionId") String institutionId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Deleting user's consent record [userId: {}, institutionId: {}]", userId, institutionId);

        UserConsent result = userConsentService.getUserConsent(userId, institutionId)
            .orElseThrow(() -> new NotFoundException("UserConsent", Map.of("userId", userId, "institutionId", institutionId)));

        userConsentService.consentCancelled(result.getId());
        return Response.noContent().build();
    }

    @POST
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response register(@Context SecurityContext ctx,
                             @PathParam("institutionId") String institutionId,
                             @Valid UserConsentRequest consentRequest) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);

        URI consentLink = userConsentService.register(userId, RailProvider.NORDIGEN, institutionId,
            consentRequest.getCallbackUri());

        log.debug("Redirecting user to bank consent [link: {}]", consentLink.toASCIIString());
        return Response.ok(consentLink).build();
    }

    /**
     * Called by the rail-service as a call-back to the requisition request. The
     * request involves user interaction (to agree to the requisition for access).
     * We pass a URI to this endpoint in the request parameters, and the rail service
     * will call this endpoint with a positive or negative outcome.
     *
     * @param headers the request headers.
     * @param consentReference the user-consent identifier to which the requisition belongs.
     * @param error an error code, provided if the consent was denied.
     * @param details an error message, provided if the consent was denied.
     * @return a redirect to the callback-uri provided by the client when the request
     * was initiated.
     */
    @GET
    @Path("/response")
    @PermitAll
    public Response consentResponse(@Context HttpHeaders headers,
                                    @QueryParam("ref") String consentReference,
                                    @QueryParam("error") String error,
                                    @QueryParam("details") String details) {
        // A typical consent callback request:
        // http://5.81.68.243/api/v1/rails/consents/response
        // ?ref=cbaee100-3f1f-4d7c-9b3b-07244e6a019f
        // &error=UserCancelledSession
        // &details=User+cancelled+the+session.

        log.info("User consent response [consentReference: {}, error: {}, details: {}]", consentReference, error, details);
        logHeaders(headers);

        URI redirectUri = ((error == null) || (error.isBlank()))
            ? userConsentService.consentGiven(consentReference)
            : userConsentService.consentDenied(consentReference, error, details);

        return Response.temporaryRedirect(redirectUri).build();
    }

    private void logHeaders(HttpHeaders headers) {
        headers.getRequestHeaders().forEach((k, v) -> log.debug("Header: {} = \"{}\"", k, v));
    }

    private UserConsentResponse marshal(UserConsent consent) {
        Institution institution = railProviderApi.getInstitution(consent.getInstitutionId())
            .orElseThrow(() -> new NotFoundException("Institution", consent.getInstitutionId()));

        return new UserConsentResponse()
            .institutionId(consent.getInstitutionId())
            .institutionName(institution.getName())
            .dateGiven(consent.getDateGiven())
            .agreementExpires(consent.getAgreementExpires())
            .maxHistory(consent.getMaxHistory())
            .status(consent.getStatus().name())
            .errorCode(consent.getErrorCode())
            .errorDetail(consent.getErrorDetail())
            .accounts(accountService.getAccountsByUserConsent(consent).stream()
                .map(this::marshal)
                .toList()
            );
    }

    private AccountSummaryResponse marshal(Account account) {
        return new AccountSummaryResponse()
            .id(account.getId())
            .name(account.getAccountName())
            .ownerName(account.getOwnerName())
            .currency(account.getCurrency().getCurrencyCode())
            .iban(account.getIban())
            .institutionId(account.getInstitutionId());
    }
}