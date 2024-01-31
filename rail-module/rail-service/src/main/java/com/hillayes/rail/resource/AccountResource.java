package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.AccountBalanceResponse;
import com.hillayes.onestop.api.AccountResponse;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedAccounts;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Path("/api/v1/rails/accounts")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AccountResource {
    private final AccountService accountService;
    private final InstitutionService institutionService;

    @GET
    public Response getAccounts(@Context SecurityContext ctx,
                                @Context UriInfo uriInfo,
                                @QueryParam("page")@DefaultValue("0") int page,
                                @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Listing accounts [userId: {}, page: {}, pageSize: {}]", userId, page, pageSize);
        Page<Account> accountsPage = accountService.getAccounts(userId, page, pageSize);

        PaginatedAccounts response = new PaginatedAccounts()
            .page(accountsPage.getPageIndex())
            .pageSize(accountsPage.getPageSize())
            .count(accountsPage.getContentSize())
            .total(accountsPage.getTotalCount())
            .items(accountsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, accountsPage));

        log.debug("Listing accounts [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            userId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountId}")
    public Response getAccountById(@Context SecurityContext ctx,
                                   @PathParam("accountId") UUID accountId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        log.info("Getting account [userId: {}, accountId: {}]", userId, accountId);
        Account account = accountService.getAccount(accountId)
            .filter(acc -> acc.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Account", accountId));

        return Response.ok(marshal(account)).build();
    }

    private AccountResponse marshal(Account account) {
        RailInstitution institution = institutionService.get(account.getInstitutionId())
            .orElseThrow(() -> new NotFoundException("Institution", account.getInstitutionId()));

        return new AccountResponse()
            .id(account.getId())
            .name(account.getAccountName())
            .ownerName(account.getOwnerName())
            .currency(account.getCurrency().getCurrencyCode())
            .iban(account.getIban())
            .institution(marshal(institution))
            .balance(accountService.getMostRecentBalance(account).stream().map(balance ->
                    new AccountBalanceResponse()
                        .id(balance.getId())
                        .amount(balance.getAmount().toDecimal())
                        .currency(balance.getAmount().getCurrencyCode())
                        .referenceDate(balance.getReferenceDate())
                        .dateRecorded(balance.getDateCreated())
                        .type(balance.getBalanceType())
                ).toList()
            );
    }

    private InstitutionResponse marshal(RailInstitution institution) {
        return new InstitutionResponse()
            .id(institution.getId())
            .name(institution.getName())
            .bic(institution.getBic())
            .logo(institution.getLogo());
    }
}
