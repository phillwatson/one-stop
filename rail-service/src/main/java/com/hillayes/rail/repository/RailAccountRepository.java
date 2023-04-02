package com.hillayes.rail.repository;

import com.hillayes.rail.model.AccountDetail;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionsResponse;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Map;

@ApplicationScoped
@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/accountDetails/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RailAccountRepository {
    @GET
    @Path("{id}/")
    public AccountDetail get(@PathParam("id") String id);

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
