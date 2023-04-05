package com.hillayes.rail.repository;

import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/requisitions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface RequisitionRepository {
    @GET
    public PaginatedList<Requisition> list(@QueryParam("limit") int limit,
                                           @QueryParam("offset") int offset);
    @GET
    @Path("{id}/")
    public Requisition get(@PathParam("id") String id);

    @POST
    public Requisition create(RequisitionRequest requisition);

    @DELETE
    @Path("{id}/")
    public Map<String, Object> delete(@PathParam("id") String id);
}
