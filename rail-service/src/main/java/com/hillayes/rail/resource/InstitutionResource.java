package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Path("/api/v1/rails/banks")
@RolesAllowed({"admin", "user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class InstitutionResource {
    private final InstitutionService institutionService;

    @GET
    public Collection<Institution> getAll(@QueryParam("country") String countryCode) {
        log.info("List institutions [country: {}]", countryCode);
        Set<Institution> result = new HashSet<>(institutionService.list(countryCode, true));
        result.addAll(institutionService.list(countryCode, false));

        log.info("List institutions [country: {}, size: {}]", countryCode, result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    public Institution getById(@PathParam("id") String id) {
        log.info("Get institution [id: {}]", id);
        return institutionService.get(id)
            .orElseThrow(() -> new NotFoundException("Institution", id));
    }
}
