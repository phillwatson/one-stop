package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
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
            .page(accountsPage.getNumber())
            .pageSize(accountsPage.getSize())
            .count(accountsPage.getNumberOfElements())
            .total(accountsPage.getTotalElements())
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
        Institution institution = institutionService.get(account.getInstitutionId())
            .orElseThrow(() -> new NotFoundException("Institution", account.getInstitutionId()));

        return new AccountResponse()
            .id(account.getId())
            .name(account.getAccountName())
            .ownerName(account.getOwnerName())
            .currency(account.getCurrencyCode())
            .iban(account.getIban())
            .institution(marshal(institution))
            .balance(accountService.getMostRecentBalance(account).stream().map(balance ->
                    new AccountBalanceResponse()
                        .id(balance.getId())
                        .amount(balance.getAmount())
                        .currency(balance.getCurrencyCode())
                        .referenceDate(balance.getReferenceDate())
                        .dateRecorded(balance.getDateCreated())
                        .type(balance.getBalanceType())
                ).toList()
            );
    }

    private InstitutionResponse marshal(Institution institution) {
        return new InstitutionResponse()
            .id(institution.id)
            .name(institution.name)
            .bic(institution.bic)
            .logo(institution.logo);
    }
}
