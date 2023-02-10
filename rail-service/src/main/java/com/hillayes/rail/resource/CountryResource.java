package com.hillayes.rail.resource;

import com.hillayes.rail.domain.Country;
import com.hillayes.rail.services.CountryService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/api/v1/rails/countries")
@RolesAllowed({"admin", "user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class CountryResource {
    @Inject
    CountryService countryService;

    @GET
    public Collection<Country> getAll() {
        log.info("List countries");
        Collection<Country> result = countryService.list();
        log.info("List countries [size: {}]", result.size());
        return result;
    }

    @GET
    @Path("/{id}")
    public Country getById(@PathParam("id") String id) {
        log.info("Get country [id: {}]", id);
        return countryService.get(id).orElseThrow(NotFoundException::new);
    }
}
