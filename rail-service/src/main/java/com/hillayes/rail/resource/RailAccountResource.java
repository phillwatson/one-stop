package com.hillayes.rail.resource;

import com.hillayes.rail.model.AccountDetail;
import com.hillayes.rail.model.AccountBalance;
import com.hillayes.rail.model.TransactionList;
import com.hillayes.rail.service.RailAccountService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    public AccountDetail get(@PathParam("id") String id) {
        log.info("Get account [id: {}]", id);
        return railAccountService.get(id);
    }

    @GET
    @Path("{id}/balances")
    public List<AccountBalance> balances(@PathParam("id") String id) {
        log.info("Get account balances [id: {}]", id);
        return railAccountService.balances(id).balances;
    }

    @GET
    @Path("/{id}/details")
    public Map<String, Object> details(@PathParam("id") String id) {
        log.info("Get account details [id: {}]", id);
        return railAccountService.details(id);
    }

    @GET
    @Path("/{id}/transactions")
    public TransactionList transactions(@PathParam("id") String id,
                                        @QueryParam("date_from") String dateFrom,
                                        @QueryParam("date_to") String dateTo) {
        log.info("Get account transactions [id: {}, dateFrom: {}, dateTo: {}]", id, dateFrom, dateTo);
        return railAccountService.transactions(id, LocalDate.parse(dateFrom), LocalDate.parse(dateTo)).transactions;
    }
}
