package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.ConsentResponse;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
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
    private final InstitutionService institutionService;
    private final AccountService accountService;
    private final RailProviderFactory railProviderFactory;

    @GET
    @RolesAllowed("user")
    public Response getConsents(@Context SecurityContext ctx,
                                @Context UriInfo uriInfo,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Listing user's consents [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);

        Page<UserConsent> consentsPage = userConsentService.listConsents(userId, page, pageSize);

        PaginatedUserConsents response = new PaginatedUserConsents()
            .page(consentsPage.getPageIndex())
            .pageSize(consentsPage.getPageSize())
            .count(consentsPage.getContentSize())
            .total(consentsPage.getTotalCount())
            .totalPages(consentsPage.getTotalPages())
            .items(consentsPage.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, consentsPage));

        log.debug("Listing user's consents [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            userId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response getConsentForInstitution(@Context SecurityContext ctx,
                                             @PathParam("institutionId") String institutionId) {
        UUID userId = AuthUtils.getUserId(ctx);
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
                                  @PathParam("institutionId") String institutionId,
                                  @QueryParam("purge") @DefaultValue("false") boolean purge) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting user's consent record [userId: {}, institutionId: {}, purge: {}]", userId, institutionId, purge);

        UserConsent consent = userConsentService.getUserConsent(userId, institutionId)
            .orElseThrow(() -> new NotFoundException("UserConsent", Map.of("userId", userId, "institutionId", institutionId)));

        userConsentService.consentCancelled(consent.getId(), purge);
        return Response.noContent().build();
    }

    @POST
    @Path("{institutionId}")
    @RolesAllowed("user")
    public Response register(@Context SecurityContext ctx,
                             @PathParam("institutionId") String institutionId,
                             @Valid UserConsentRequest consentRequest) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Registering user's bank [userId: {}, institutionId: {}]", userId, institutionId);

        URI consentLink = userConsentService.register(userId, institutionId, consentRequest.getCallbackUri());

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
     * @param uriInfo the request URI information from which the consent reference is extracted.
     * @param railProvider the rail-provider identifier.
     * @return a redirect to the callback-uri provided by the client when the request
     * was initiated.
     */
    @GET
    @Path("/response/{railProvider}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @PermitAll
    public Response consentResponse(@Context HttpHeaders headers,
                                    @Context UriInfo uriInfo,
                                    @PathParam("railProvider") RailProvider railProvider) {
        log.info("User consent response [railProvider: {}]", railProvider);
        logHeaders(headers);

        // ask rail to extract the consent details from the query parameters
        RailProviderApi railProviderApi = railProviderFactory.get(railProvider);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(true);
        ConsentResponse consentResponse = railProviderApi.parseConsentResponse(queryParameters);

        URI redirectUri = consentResponse.isError()
            ? userConsentService.consentDenied(railProviderApi, consentResponse)
            : userConsentService.consentGiven(railProviderApi, consentResponse);

        return Response.temporaryRedirect(redirectUri).build();
    }

    private void logHeaders(HttpHeaders headers) {
        headers.getRequestHeaders().forEach((k, v) -> log.debug("Header: {} = \"{}\"", k, v));
    }

    private UserConsentResponse marshal(UserConsent consent) {
        RailInstitution institution = institutionService.get(consent.getProvider(), consent.getInstitutionId())
            .orElseThrow(() -> new NotFoundException("Institution", consent.getInstitutionId()));

        return new UserConsentResponse()
            .id(consent.getId())
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
