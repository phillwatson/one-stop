package com.hillayes.nordigen.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.nordigen.model.AccountSummary;
import com.hillayes.nordigen.model.Balance;
import com.hillayes.nordigen.model.TransactionList;
import com.hillayes.nordigen.service.AccountService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Path("/api/v1/rails/nordigen/accounts")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class AccountAdminResource {
    private final AccountService accountService;

    @GET
    @Path("{id}")
    public AccountSummary getAccount(@PathParam("id") String id) {
        log.info("Get account [id: {}]", id);
        return accountService.get(id)
            .orElseThrow(() -> new NotFoundException("RailAccount", id));
    }

    @GET
    @Path("{id}/balances")
    public List<Balance> getBalances(@PathParam("id") String id) {
        log.info("Get account balances [id: {}]", id);
        return accountService.balances(id)
            .orElseThrow(() -> new NotFoundException("RailAccount", id));
    }

    @GET
    @Path("{id}/details")
    public Map<String, Object> getAccountDetails(@PathParam("id") String id) {
        log.info("Get account details [id: {}]", id);
        return accountService.details(id)
            .orElseThrow(() -> new NotFoundException("RailAccount", id));
    }

    @GET
    @Path("{id}/transactions")
    public TransactionList getTransactions(@PathParam("id") String id,
                                           @QueryParam("date_from") String dateFrom,
                                           @QueryParam("date_to") String dateTo) {
        log.info("Get account transactions [id: {}, dateFrom: {}, dateTo: {}]", id, dateFrom, dateTo);
        LocalDate from = dateFrom == null ? null : LocalDate.parse(dateFrom);
        LocalDate to = dateTo == null ? null : LocalDate.parse(dateTo);
        return accountService.transactions(id, from, to)
            .orElseThrow(() -> new NotFoundException("RailAccount", id));
    }
}
