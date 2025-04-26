package com.hillayes.nordigen.api;

import com.hillayes.nordigen.model.Institution;
import com.hillayes.nordigen.model.InstitutionDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "nordigen-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Path("/api/v2/institutions/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface InstitutionApi {
    @GET
    public List<Institution> list(@QueryParam("country") String countryCode);

    @GET
    @Path("{id}/")
    public InstitutionDetail get(@PathParam("id") String id);
}
