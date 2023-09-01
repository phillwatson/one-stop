package com.hillayes.sim.nordigen;

import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionRequest;
import com.hillayes.nordigen.model.RequisitionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Path(NordigenSimulator.BASE_URI + "/api/v2/requisitions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class RequisitionsEndpoint extends AbstractEndpoint {
    private final AgreementsEndpoint agreements;

    private final AccountsEndpoint accounts;

    private final Map<String, Requisition> requisitions = new HashMap<>();

    protected RequisitionsEndpoint(AgreementsEndpoint agreements,
                                   AccountsEndpoint accounts) {
        this.agreements = agreements;
        this.accounts = accounts;
    }

    public void reset() {
        requisitions.clear();
    }

    @GET
    public Response list(@QueryParam("offset") int offset,
                         @QueryParam("limit") int limit) {
        log.info("list requisitions [offset: {}, limit: {}]", offset, limit);
        return Response.ok(sublist(offset, limit, requisitions.values())).build();
    }

    /**
     * Returns the identified Requisition. Repeated calls will transition the
     * requisition's status to the next status in the requisition flow.
     */
    @GET
    @Path("{id}/")
    public Response get(@PathParam("id") String id) {
        log.info("get requisitions [id: {}]", id);
        Requisition entity = requisitions.get(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        RequisitionStatus nextStatus = entity.status.nextStatus();
        if (nextStatus != null) {
            entity.status = nextStatus;

            // if accounts have been linked
            if (entity.status == RequisitionStatus.LN) {
                // mock the accounts
                entity.accounts = List.of(
                    accounts.acquireAccount(entity.institutionId),
                    accounts.acquireAccount(entity.institutionId)
                );
            }
        }
        return Response.ok(entity).build();
    }

    @POST
    public Response create(RequisitionRequest requisitionRequest) {
        log.info("create requisitions [institution: {}]", requisitionRequest.getInstitutionId());
        return requisitions.values().stream()
            .filter(agreement -> agreement.institutionId.equals(requisitionRequest.getInstitutionId()))
            .findFirst()
            .map(agreement -> Response.status(Response.Status.CONFLICT)
                .entity(Map.of(
                    "summary", "Conflict",
                    "detail", "An requisition with this Institution already exists.",
                    "status_code", 409
                ))
                .build()
            )
            .orElseGet(() -> {
                    String id = UUID.randomUUID().toString();
                    Requisition response = Requisition.builder()
                        .id(id)
                        .created(OffsetDateTime.now())
                        .status(RequisitionStatus.CR)
                        .link("https://ob.nordigen.com/psd2/start/" + id + "/" + requisitionRequest.getInstitutionId())
                        .redirect(requisitionRequest.getRedirect())
                        .institutionId(requisitionRequest.getInstitutionId())
                        .agreement(requisitionRequest.getAgreement())
                        .reference(requisitionRequest.getReference())
                        .userLanguage(requisitionRequest.getUserLanguage())
                        .ssn(requisitionRequest.getSsn())
                        .accountSelection(requisitionRequest.getAccountSelection())
                        .redirectImmediate(requisitionRequest.getRedirectImmediate())
                        .accounts(List.of())
                        .build();

                    requisitions.put(response.id, response);
                    return Response.status(Response.Status.CREATED).entity(response).build();
                }
            );
    }

    @DELETE
    @Path("{id}/")
    public Response delete(@PathParam("id") String id) {
        log.info("delete requisitions [id: {}]", id);
        Requisition entity = requisitions.remove(id);

        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // delete associated agreements
        agreements.removeAgreement(entity.agreement);
        entity.accounts.forEach(accounts::removeAccount);

        return Response.ok(Map.of(
                "status_code", 200,
                "summary", "Requisition deleted",
                "detail", "Requisition " + id + " deleted"
            ))
            .build();
    }
}
