package com.hillayes.rail.services;

import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.ws.rs.*;
import java.util.Map;
import java.util.UUID;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/requisitions/")
@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
public interface RequisitionService {
    @GET
    public PaginatedList<Requisition> list(@QueryParam("limit") int limit,
                                           @QueryParam("offset") int offset);
    @GET
    @Path("{id}/")
    public Requisition get(@PathParam("id") UUID id);

    @POST
    public Requisition create(RequisitionRequest requisition);

    @DELETE
    @Path("{id}/")
    public Map<String, Object> delete(@PathParam("id") UUID id);
}
