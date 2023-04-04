package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.model.Requisition;
import com.hillayes.rail.model.RequisitionRequest;
import com.hillayes.rail.service.RequisitionService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/api/v1/rails/requisitions")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class RequisitionResource {
    @Inject
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
    @Path("/{id}")
    public Requisition get(@PathParam("id") String id) {
        log.info("Get requisition [id: {}]", id);
        return requisitionService.get(id)
            .orElseThrow(() -> new NotFoundException("Requisition", id));
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
    @Path("/{id}")
    public Map<String, Object> delete(@PathParam("id") String id) {
        log.info("Delete requisition [id: {}]", id);
        return requisitionService.delete(id);
    }
}
