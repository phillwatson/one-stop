package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/agreements/enduser/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface AgreementApi {
    @GET
    public PaginatedList<EndUserAgreement> list(@QueryParam("limit") int limit,
                                                @QueryParam("offset") int offset);

    @POST
    public EndUserAgreement create(EndUserAgreementRequest agreement);

    @PUT
    @Path("{id}/accept/")
    public EndUserAgreement accept(@PathParam("id") String id,
                                   EndUserAgreementAccepted acceptance);

    @GET
    @Path("{id}/")
    public EndUserAgreement get(@PathParam("id") String id);

    @DELETE
    @Path("{id}/")
    public Map<String,Object> delete(@PathParam("id") String id);

    @GET
    @Path("{id}/reconfirm/")
    public ReconfirmationRetrieve getReconfirmation(@PathParam("id") String agreementId);

    @PUT
    @Path("{id}/reconfirm/")
    public ReconfirmationRetrieve createReconfirmation(@PathParam("id") String agreementId,
                                                      ReconfirmationRetrieveRequest request);
}
