package com.hillayes.rail.repository;

import com.hillayes.nordigen.model.EndUserAgreement;
import com.hillayes.nordigen.model.EndUserAgreementAccepted;
import com.hillayes.nordigen.model.EndUserAgreementRequest;
import com.hillayes.nordigen.model.PaginatedList;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/agreements/enduser/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface AgreementRepository {
    @GET
    public PaginatedList<EndUserAgreement> list(@QueryParam("limit") int limit,
                                                @QueryParam("offset") int offset);

    @POST
    public EndUserAgreement create(EndUserAgreementRequest agreement);

    @PUT
    @Path("{id}/")
    public EndUserAgreement accept(@PathParam("id") String id,
                                   EndUserAgreementAccepted acceptance);

    @GET
    @Path("{id}/")
    public EndUserAgreement get(@PathParam("id") String id);

    @DELETE
    @Path("{id}/")
    public Map<String,Object> delete(@PathParam("id") String id);
}
