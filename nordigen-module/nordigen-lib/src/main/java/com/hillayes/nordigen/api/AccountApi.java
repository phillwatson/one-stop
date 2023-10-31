package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.AccountBalanceList;
import com.hillayes.nordigen.model.AccountSummary;
import com.hillayes.nordigen.model.TransactionsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDate;
import java.util.Map;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/accounts/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AccountApi {
    @GET
    @Path("{id}/")
    public AccountSummary get(@PathParam("id") String id);

    @GET
    @Path("{id}/balances/")
    public AccountBalanceList balances(@PathParam("id") String id);

    @GET
    @Path("{id}/details/")
    public Map<String,Object> details(@PathParam("id") String id);

    @GET
    @Path("{id}/transactions/")
    public TransactionsResponse transactions(@PathParam("id") String id,
                                             @QueryParam("date_from") LocalDate dateFrom,
                                             @QueryParam("date_to") LocalDate dateTo);
}
