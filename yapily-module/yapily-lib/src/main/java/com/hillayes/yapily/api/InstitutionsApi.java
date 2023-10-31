package com.hillayes.yapily.api;

import com.hillayes.yapily.model.ApiListResponseOfInstitution;
import com.hillayes.yapily.model.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "yapily-api")
@RegisterClientHeaders(BearerHeaderFactory.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface InstitutionsApi {
    @GET
    @Path("institutions")
    public ApiListResponseOfInstitution getInstitutions();

    @GET
    @Path("institutions/{id}")
    public Institution getInstitution(@PathParam("id") String id);
}
