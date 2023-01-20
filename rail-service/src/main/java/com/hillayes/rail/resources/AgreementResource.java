package com.hillayes.rail.resources;

import com.hillayes.rail.services.AgreementService;
import com.hillayes.rail.services.model.EndUserAgreement;
import com.hillayes.rail.services.model.EndUserAgreementAccepted;
import com.hillayes.rail.services.model.EndUserAgreementRequest;
import com.hillayes.rail.services.model.PaginatedList;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/agreements")
@Produces("application/json")
@Consumes("application/json")
@Slf4j
public class AgreementResource {
    @Inject
    @RestClient
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
    @Path("{id}/")
    public EndUserAgreement accept(@PathParam("id") UUID id,
                                   EndUserAgreementAccepted acceptance) {
        log.info("Accept agreement [id: {}]", id);
        return agreementService.accept(id, acceptance);
    }

    @GET
    @Path("{id}/")
    public EndUserAgreement get(@PathParam("id") UUID id) {
        log.info("Get agreement [id: {}]", id);
        return agreementService.get(id);
    }

    @DELETE
    @Path("{id}/")
    public Map<String,Object> delete(@PathParam("id") UUID id) {
        log.info("Delete agreement [id: {}]", id);
        return agreementService.delete(id);
    }
}
