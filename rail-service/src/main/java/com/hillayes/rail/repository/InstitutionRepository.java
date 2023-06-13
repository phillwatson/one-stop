package com.hillayes.rail.repository;

import com.hillayes.rail.model.Institution;
import com.hillayes.rail.model.InstitutionDetail;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/institutions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface InstitutionRepository {
    @GET
    public List<Institution> list(@QueryParam("country") String countryCode,
                                  @QueryParam("payments_enabled") Boolean paymentsEnabled);
    @GET
    @Path("{id}/")
    public InstitutionDetail get(@PathParam("id") String id);
}
