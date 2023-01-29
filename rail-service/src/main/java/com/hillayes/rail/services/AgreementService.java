package com.hillayes.rail.services;

import com.hillayes.rail.model.EndUserAgreement;
import com.hillayes.rail.model.EndUserAgreementAccepted;
import com.hillayes.rail.model.EndUserAgreementRequest;
import com.hillayes.rail.model.PaginatedList;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import java.util.Map;
import java.util.UUID;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/agreements/enduser/")
@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
public interface AgreementService {
    @GET
    public PaginatedList<EndUserAgreement> list(@QueryParam("limit") int limit,
                                                @QueryParam("offset") int offset);

    @POST
    public EndUserAgreement create(EndUserAgreementRequest agreement);

    @PUT
    @Path("{id}/")
    public EndUserAgreement accept(@PathParam("id") UUID id,
                                   EndUserAgreementAccepted acceptance);

    @GET
    @Path("{id}/")
    public EndUserAgreement get(@PathParam("id") UUID id);

    @DELETE
    @Path("{id}/")
    public Map<String,Object> delete(@PathParam("id") UUID id);
}
