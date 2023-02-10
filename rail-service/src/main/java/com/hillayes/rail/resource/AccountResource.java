package com.hillayes.rail.resource;

import com.hillayes.rail.services.AccountService;
import com.hillayes.rail.model.Account;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionList;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/rails/accounts")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AccountResource {
    @Inject
    @RestClient
    AccountService accountService;

    @GET
    @Path("/{id}")
    public Account get(@PathParam("id") UUID id) {
        log.info("Get account [id: {}]", id);
        return accountService.get(id);
    }

    @GET
    @Path("{id}/balances")
    public AccountBalanceList balances(@PathParam("id") UUID id) {
        log.info("Get account balances [id: {}]", id);
        return accountService.balances(id);
    }

    @GET
    @Path("/{id}/details")
    public Map<String,Object> details(@PathParam("id") UUID id) {
        log.info("Get account details [id: {}]", id);
        return accountService.details(id);
    }

    @GET
    @Path("/{id}/transactions")
    public TransactionList transactions(@PathParam("id") UUID id,
                                        @QueryParam("date_from") LocalDate dateFrom,
                                        @QueryParam("date_to") LocalDate dateTo) {
        log.info("Get account transactions [id: {}]", id);
        return accountService.transactions(id, dateFrom, dateTo);
    }
}
