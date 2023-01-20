package com.hillayes.rail.resources;

import com.hillayes.rail.services.RequisitionService;
import com.hillayes.rail.services.model.PaginatedList;
import com.hillayes.rail.services.model.Requisition;
import com.hillayes.rail.services.model.RequisitionRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/requisitions")
@Produces("application/json")
@Consumes("application/json")
@Slf4j
public class RequisitionResource {
    @Inject
    @RestClient
    RequisitionService requisitionService;
    @GET
    public PaginatedList<Requisition> list(@QueryParam("limit") int limit,
                                           @QueryParam("offset") int offset) {
        log.info("List requisitions [limit: {}, offset: {}]", limit, offset);
        PaginatedList<Requisition> result = requisitionService.list(limit, offset);
        log.info("List requisitions [limit: {}, offset: {}, count: {}]", limit, offset, result.count);
        return result;
    }

    @GET
    @Path("{id}/")
    public Requisition get(@PathParam("id") UUID id) {
        log.info("Get requisition [id: {}]", id);
        return requisitionService.get(id);
    }

    @POST
    public Response create(RequisitionRequest requisition) {
        log.info("Create requisition [reference: {}, agreement: {}, institution: {}]",
                requisition.getReference(), requisition.getAgreement(), requisition.getInstitutionId());
        Requisition result = requisitionService.create(requisition);
        log.info("Created requisition [reference: {}, agreement: {}, institution: {}, id: {}]",
                requisition.getReference(), requisition.getAgreement(), requisition.getInstitutionId(), result.id);
        return Response
                .status(Response.Status.CREATED)
                .entity(result)
                .build();
    }

    @DELETE
    @Path("{id}/")
    public Map<String, Object> delete(@PathParam("id") UUID id) {
        log.info("Delete requisition [id: {}]", id);
        return requisitionService.delete(id);
    }
}
