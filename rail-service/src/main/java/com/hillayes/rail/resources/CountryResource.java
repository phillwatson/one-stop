package com.hillayes.rail.resources;

import com.hillayes.rail.repository.domain.Country;
import com.hillayes.rail.services.CountryService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.Collection;

@Path("/api/v1/countries")
@Produces("application/json")
@Consumes("application/json")
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
