package com.hillayes.rail.resource.admin;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.resource.ResourceUtils;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.service.UserConsentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

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

        PaginatedUserConsentsAdmin response = new PaginatedUserConsentsAdmin()
            .page(consentsPage.getNumber())
            .pageSize(consentsPage.getSize())
            .count(consentsPage.getNumberOfElements())
            .total(consentsPage.getTotalElements())
            .items(consentsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, consentsPage));

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

        UserConsentAdminResponse result = marshal(consent);

        log.debug("Getting user's consent record [consentId: {}]", consentId);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("{consentId}")
    public Response deleteConsent(@PathParam("consentId") UUID consentId) {
        log.info("Deleting user's consent record [consentId: {}]", consentId);

        UserConsent result = userConsentService.getUserConsent(consentId)
            .orElseThrow(() -> new NotFoundException("UserConsent", consentId));

        userConsentService.consentCancelled(result.getId());
        return Response.noContent().build();
    }

    private UserConsentAdminResponse marshal(UserConsent consent) {
        Institution institution = institutionService.get(consent.getInstitutionId())
            .orElseThrow(() -> new NotFoundException("Institution", consent.getInstitutionId()));

        return new UserConsentAdminResponse()
            .id(consent.getId())
            .institutionId(consent.getInstitutionId())
            .institutionName(institution.name)
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
            .currency(account.getCurrencyCode())
            .iban(account.getIban())
            .institutionId(account.getInstitutionId());
    }
}
