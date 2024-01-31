package com.hillayes.nordigen.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.EndUserAgreementAccepted;
import com.hillayes.nordigen.model.EndUserAgreementRequest;
import com.hillayes.nordigen.model.PaginatedList;
import com.hillayes.nordigen.service.AgreementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Path("/api/v1/rails/nordigen/agreements")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AgreementAdminResource {
    @Inject
    private AgreementService agreementService;

    @GET
    public PaginatedList<EndUserAgreement> list(@QueryParam("limit") int limit,
                                                @QueryParam("offset") int offset) {
        log.info("List agreements [limit: {}, offset: {}]", limit, offset);
        PaginatedList<EndUserAgreement> result = agreementService.list(limit, offset);
        log.debug("List agreements [limit: {}, offset: {}, count: {}]", limit, offset, result.count);
        return result;
    }

    @POST
    public Response create(@Valid EndUserAgreementRequest agreement) {
        log.info("Create agreement [institution: {}, scope: {}]", agreement.getInstitutionId(), agreement.getAccessScope());
        EndUserAgreement result = agreementService.create(agreement);
        log.debug("Created agreement [institution: {}, id: {}]", agreement.getInstitutionId(), result.id);
        return Response
                .status(Response.Status.CREATED)
                .entity(result)
                .build();
    }

    @PUT
    @Path("/{id}")
    public EndUserAgreement accept(@PathParam("id") String id,
                                   @Valid EndUserAgreementAccepted acceptance) {
        log.info("Accept agreement [id: {}]", id);
        return agreementService.accept(id, acceptance)
            .orElseThrow(() -> new NotFoundException("Agreement", id));
    }

    @GET
    @Path("/{id}")
    public EndUserAgreement get(@PathParam("id") String id) {
        log.info("Get agreement [id: {}]", id);
        return agreementService.get(id)
            .orElseThrow(() -> new NotFoundException("Agreement", id));
    }

    @DELETE
    @Path("/{id}")
    public Map<String,Object> delete(@PathParam("id") String id) {
        log.info("Delete agreement [id: {}]", id);
        return agreementService.delete(id);
    }
}
