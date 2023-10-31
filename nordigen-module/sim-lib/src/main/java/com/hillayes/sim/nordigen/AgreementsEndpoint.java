package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.EndUserAgreementAccepted;
import com.hillayes.nordigen.model.EndUserAgreementRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Path(NordigenSimulator.BASE_URI + "/api/v2/agreements/enduser/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AgreementsEndpoint extends AbstractEndpoint {
    private final Map<String, EndUserAgreement> agreements = new HashMap<>();

    EndUserAgreement removeAgreement(String id) {
        return agreements.remove(id);
    }

    public void reset() {
        agreements.clear();
    }

    @GET
    public Response list(@QueryParam("offset") int offset,
                         @QueryParam("limit") int limit) {
        log.info("listing agreements [offset: {}, limit: {}]", offset, limit);
        return Response.ok(sublist(offset, limit, agreements.values())).build();
    }

    @POST
    public Response create(EndUserAgreementRequest agreementRequest) {
        log.info("create agreement [institution: {}]", agreementRequest.getInstitutionId());
        return agreements.values().stream()
            .filter(agreement -> agreement.institutionId.equals(agreementRequest.getInstitutionId()))
            .findFirst()
            .map(agreement -> Response
                .status(Response.Status.CONFLICT)
                .entity(Map.of(
                    "summary", "Conflict",
                    "detail", "An agreement with this Institution already exists.",
                    "status_code", 409
                )).build()
            )
            .orElseGet(() ->
                {
                    EndUserAgreement response = new EndUserAgreement();
                    response.id = UUID.randomUUID().toString();
                    response.maxHistoricalDays = agreementRequest.getMaxHistoricalDays();
                    response.accessValidForDays = agreementRequest.getAccessValidForDays();
                    response.accessScope = agreementRequest.getAccessScope();
                    response.institutionId = agreementRequest.getInstitutionId();
                    response.created = OffsetDateTime.now();
                    response.accepted = null;

                    agreements.put(response.id, response);
                    return Response.status(Response.Status.CREATED).entity(response).build();
                }
            );
    }

    @PUT
    @Path("{id}/")
    public Response accept(@PathParam("id") String id,
                           EndUserAgreementAccepted acceptance) {
        log.info("accept agreement [id: {}]", id);
        return Response.status(Response.Status.FORBIDDEN).entity(Map.of(
            "summary", "Insufficient permissions",
            "detail", "Your company doesn't have permission to accept EUA. You'll have to use our default form for this action.",
            "status_code", 403
        )).build();
    }

    @GET
    @Path("{id}/")
    public Response get(@PathParam("id") String id) {
        log.info("get agreement [id: {}]", id);
        EndUserAgreement entity = agreements.get(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entity).build();
    }

    @DELETE
    @Path("{id}/")
    public Response delete(@PathParam("id") String id) {
        log.info("delete agreement [id: {}]", id);
        EndUserAgreement entity = removeAgreement(id);

        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(Map.of(
                "status_code", 200,
                "summary", "End User Agreement deleted",
                "detail", "End User Agreement " + id + " deleted"
            )).build();
    }
}
