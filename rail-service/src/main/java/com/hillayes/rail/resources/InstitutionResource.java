package com.hillayes.rail.resources;

import com.hillayes.rail.model.Institution;
import com.hillayes.rail.services.InstitutionService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;

@Path("/api/v1/banks")
@Produces("application/json")
@Consumes("application/json")
@Slf4j
public class InstitutionResource {
    @Inject
    @RestClient
    InstitutionService institutionService;

    @GET
    public List<Institution> getAll(@QueryParam("country") String countryCode,
                                    @QueryParam("payments_enabled") Boolean paymentsEnabled) {
        log.info("List institutions [country: {}, payment: {}]", countryCode, paymentsEnabled);
        List<Institution> result = institutionService.list(countryCode, paymentsEnabled);
        log.info("List institutions [country: {}, payment: {}, size: {}]", countryCode, paymentsEnabled, result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    public Institution getById(@PathParam("id") String id) {
        log.info("Get institution [id: {}]", id);
        return institutionService.get(id);
    }
}
