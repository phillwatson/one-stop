package com.hillayes.rail.services;

import com.hillayes.rail.services.model.Account;
import com.hillayes.rail.services.model.AccountBalanceList;
import com.hillayes.rail.services.model.TransactionList;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Singleton;
import javax.ws.rs.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/accounts/")
@Produces("application/json")
@Consumes("application/json")
@Singleton
public interface AccountService {
    @GET
    @Path("{id}/")
    public Account get(@PathParam("id") UUID id);

    @GET
    @Path("{id}/balances/")
    public AccountBalanceList balances(@PathParam("id") UUID id);

    @GET
    @Path("{id}/details/")
    public Map<String,Object> details(@PathParam("id") UUID id);

    @GET
    @Path("{id}/transactions/")
    public TransactionList transactions(@PathParam("id") UUID id,
                                        @QueryParam("date_from") LocalDate dateFrom,
                                        @QueryParam("date_to") LocalDate dateTo);
}
