package com.hillayes.rail.resource;

import com.hillayes.rail.model.Account;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionList;
import com.hillayes.rail.services.RailAccountService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/rails/rails-accounts")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class RailAccountResource {
    @Inject
    RailAccountService railAccountService;

    @GET
    @Path("/{id}")
    public Account get(@PathParam("id") UUID id) {
        log.info("Get account [id: {}]", id);
        return railAccountService.get(id);
    }

    @GET
    @Path("{id}/balances")
    public AccountBalanceList balances(@PathParam("id") UUID id) {
        log.info("Get account balances [id: {}]", id);
        return railAccountService.balances(id);
    }

    @GET
    @Path("/{id}/details")
    public Map<String,Object> details(@PathParam("id") UUID id) {
        log.info("Get account details [id: {}]", id);
        return railAccountService.details(id);
    }

    @GET
    @Path("/{id}/transactions")
    public TransactionList transactions(@PathParam("id") UUID id,
                                        @QueryParam("date_from") String dateFrom,
                                        @QueryParam("date_to") String dateTo) {
        log.info("Get account transactions [id: {}, dateFrom: {}, dateTo: {}]", id, dateFrom, dateTo);
        return railAccountService.transactions(id, LocalDate.parse(dateFrom), LocalDate.parse(dateTo));
    }
}
