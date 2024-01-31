package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/requisitions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface RequisitionApi {
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
