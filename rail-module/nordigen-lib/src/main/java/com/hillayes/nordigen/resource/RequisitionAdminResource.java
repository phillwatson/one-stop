package com.hillayes.nordigen.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionRequest;
import com.hillayes.nordigen.service.RequisitionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Path("/api/v1/rails/nordigen/requisitions")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class RequisitionAdminResource {
    private final RequisitionService requisitionService;

    @GET
    public PaginatedList<Requisition> list(@QueryParam("limit") int limit,
                                           @QueryParam("offset") int offset) {
        log.info("List requisitions [limit: {}, offset: {}]", limit, offset);
        PaginatedList<Requisition> result = requisitionService.list(limit, offset);
        log.debug("List requisitions [limit: {}, offset: {}, count: {}]", limit, offset, result.count);
        return result;
    }

    @GET
    @Path("{id}")
    public Requisition get(@PathParam("id") String id) {
        log.info("Get requisition [id: {}]", id);
        return requisitionService.get(id)
            .orElseThrow(() -> new NotFoundException("Requisition", id));
    }

    @POST
    public Response create(@Valid RequisitionRequest requisition) {
        log.info("Create requisition [reference: {}, agreement: {}, institution: {}]",
            requisition.getReference(), requisition.getAgreement(), requisition.getInstitutionId());
        Requisition result = requisitionService.create(requisition);
        log.debug("Created requisition [reference: {}, agreement: {}, institution: {}, id: {}]",
            requisition.getReference(), requisition.getAgreement(), requisition.getInstitutionId(), result.id);
        return Response
            .status(Response.Status.CREATED)
            .entity(result)
            .build();
    }

    @DELETE
    @Path("{id}")
    public Map<String, Object> delete(@PathParam("id") String id) {
        log.info("Delete requisition [id: {}]", id);
        return requisitionService.delete(id);
    }
}
