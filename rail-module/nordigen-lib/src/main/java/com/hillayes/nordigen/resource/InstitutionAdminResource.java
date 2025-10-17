package com.hillayes.nordigen.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.nordigen.model.Institution;
import com.hillayes.nordigen.model.InstitutionDetail;
import com.hillayes.nordigen.service.InstitutionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Path("/api/v1/rails/nordigen/institutions")
@RolesAllowed("admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InstitutionAdminResource {
    @Inject
    InstitutionService institutionService;

    @GET
    public List<Institution> listInstitutions(@QueryParam("country") String countryCode,
                                              @QueryParam("paymentsEnabled") Boolean paymentsEnabled) {
        log.info("List institutions [country:{}, paymentsEnabled: {}]", countryCode, paymentsEnabled);
        return institutionService.list(countryCode, paymentsEnabled);
    }

    @GET
    @Path("{id}")
    public InstitutionDetail getInstitution(@PathParam("id") String id) {
        log.info("Get account [id: {}]", id);
        return institutionService.get(id)
            .orElseThrow(() -> new NotFoundException("Institution", id));
    }
}
