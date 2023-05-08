package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.AccountResponse;
import com.hillayes.onestop.api.BankResponse;
import com.hillayes.onestop.api.PaginatedAccounts;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
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
        Page<Account> accountsPage = accountService.getAccounts(ResourceUtils.getUserId(ctx), page, pageSize);

        PaginatedAccounts response = new PaginatedAccounts()
            .page(accountsPage.getNumber())
            .pageSize(accountsPage.getSize())
            .count(accountsPage.getNumberOfElements())
            .total(accountsPage.getTotalElements())
            .items(accountsPage.getContent().stream().map(this::marshal).toList())
            .links(ResourceUtils.buildPageLinks(uriInfo, accountsPage));

        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountId}")
    public Response getAccountById(@Context SecurityContext ctx,
                                   @PathParam("accountId") UUID accountId) {
        UUID userId = ResourceUtils.getUserId(ctx);
        Account account = accountService.getAccount(accountId)
            .filter(acc -> acc.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Account", accountId));

        return Response.ok(marshal(account)).build();
    }

    private AccountResponse marshal(Account account) {
        BankResponse bank = institutionService.get(account.getInstitutionId())
            .map(this::marshal)
            .orElse(null);

        return new AccountResponse()
            .id(account.getId())
            .name(account.getAccountName() == null ? account.getOwnerName() : account.getAccountName())
            .iban(account.getIban())
            .bank(bank);
    }

    private BankResponse marshal(Institution institution) {
        return new BankResponse()
            .id(institution.id)
            .name(institution.name)
            .bic(institution.bic)
            .logo(institution.logo);
    }
}
