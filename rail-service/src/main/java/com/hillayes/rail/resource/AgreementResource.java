package com.hillayes.rail.resource;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import com.hillayes.rail.services.AgreementService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/rails/agreements")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AgreementResource {
    @Inject
    AgreementService agreementService;

    @GET
    public PaginatedList<EndUserAgreement> list(@QueryParam("limit") int limit,
                                                @QueryParam("offset") int offset) {
        log.info("List agreements [limit: {}, offset: {}]", limit, offset);
        PaginatedList<EndUserAgreement> result = agreementService.list(limit, offset);
        log.info("List agreements [limit: {}, offset: {}, count: {}]", limit, offset, result.count);
        return result;
    }

    @POST
    public Response create(EndUserAgreementRequest agreement) {
        log.info("Create agreement [institution: {}, scope: {}]", agreement.getInstitutionId(), agreement.getAccessScope());
        EndUserAgreement result = agreementService.create(agreement);
        log.info("Created agreement [institution: {}, id: {}]", agreement.getInstitutionId(), result.id);
        return Response
                .status(Response.Status.CREATED)
                .entity(result)
                .build();
    }

    @PUT
    @Path("/{id}")
    public EndUserAgreement accept(@PathParam("id") UUID id,
                                   EndUserAgreementAccepted acceptance) {
        log.info("Accept agreement [id: {}]", id);
        return agreementService.accept(id, acceptance);
    }

    @GET
    @Path("/{id}")
    public EndUserAgreement get(@PathParam("id") UUID id) {
        log.info("Get agreement [id: {}]", id);
        return agreementService.get(id);
    }

    @DELETE
    @Path("/{id}")
    public Map<String,Object> delete(@PathParam("id") UUID id) {
        log.info("Delete agreement [id: {}]", id);
        return agreementService.delete(id);
    }
}
