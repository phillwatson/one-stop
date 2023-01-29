package com.hillayes.rail.services;

import com.hillayes.rail.model.Institution;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import java.util.List;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/institutions/")
@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
public interface InstitutionService {
    @GET
    public List<Institution> list(@QueryParam("country") String countryCode,
                                  @QueryParam("payments_enabled") Boolean paymentsEnabled);
    @GET
    @Path("{id}/")
    public Institution get(@PathParam("id") String id);
}
