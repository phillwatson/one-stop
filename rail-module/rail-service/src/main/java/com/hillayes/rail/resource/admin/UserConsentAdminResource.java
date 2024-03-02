package com.hillayes.rail.resource.admin;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.service.UserConsentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Path("/api/v1/rails/admin/consents")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class UserConsentAdminResource {
    private final UserConsentService userConsentService;
    private final InstitutionService institutionService;
    private final AccountService accountService;

    @GET
    public Response getConsents(@Context UriInfo uriInfo,
                                @PathParam("userId") UUID userId,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("Listing user's banks [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);

        Page<UserConsent> consentsPage = userConsentService.listConsents(userId, page, pageSize);

        PaginatedUserConsents response = new PaginatedUserConsents()
            .page(consentsPage.getPageIndex())
            .pageSize(consentsPage.getPageSize())
            .count(consentsPage.getContentSize())
            .total(consentsPage.getTotalCount())
            .totalPages(consentsPage.getTotalPages())
            .items(consentsPage.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, consentsPage));

        log.debug("Listing user's banks [userId: {}, page: {}, pageSize: {}, size: {}]",
            userId, page, pageSize, response.getCount());
        return Response.ok(response).build();
    }

    @GET
    @Path("{consentId}")
    public Response getConsent(@PathParam("consentId") UUID consentId) {
        log.info("Getting user's consent record [consentId: {}]", consentId);

        UserConsent consent = userConsentService.getUserConsent(consentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", consentId));

        UserConsentResponse result = marshal(consent);

        log.debug("Getting user's consent record [consentId: {}]", consentId);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("{consentId}")
    public Response deleteConsent(@PathParam("consentId") UUID consentId) {
        log.info("Deleting user's consent record [consentId: {}]", consentId);

        UserConsent result = userConsentService.getUserConsent(consentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", consentId));

        userConsentService.consentCancelled(result.getId(), true);
        return Response.noContent().build();
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
