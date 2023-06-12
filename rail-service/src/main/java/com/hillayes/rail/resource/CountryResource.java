package com.hillayes.rail.resource;

import com.hillayes.onestop.api.CountryResponse;
import com.hillayes.onestop.api.PaginatedCountries;
import com.hillayes.rail.domain.Country;
import com.hillayes.rail.service.CountryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Collection;
import java.util.List;

@Path("/api/v1/rails/countries")
@RolesAllowed({"admin", "user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class CountryResource {
    private final CountryService countryService;

    @GET
    public Response getAll(@Context UriInfo uriInfo,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("List countries");

        Collection<Country> allCountries = countryService.list();
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();

        // reduce the overall list to the subset identified by the page parameters
        List<CountryResponse> countryPage = allCountries.stream()
            .sorted()
            .skip(((long) page) * pageSize)
            .limit(pageSize)
            .map(country -> marshal(country, uriBuilder))
            .toList();

        // convert the subset to a paginated response
        int totalPages = (int) Math.ceil((double) allCountries.size() / pageSize);
        PaginatedCountries response = new PaginatedCountries()
            .page(page)
            .pageSize(pageSize)
            .count(countryPage.size())
            .total((long) allCountries.size())
            .items(countryPage)
            .links(ResourceUtils.buildPageLinks(uriInfo, page, pageSize, totalPages));

        log.debug("List countries [page: {}, pageSize: {}, count: {}]",
            page, pageSize, response.getCount());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@Context UriInfo uriInfo,
                            @PathParam("id") String id) {
        log.info("Get country [id: {}]", id);
        Country country = countryService.get(id).orElseThrow(NotFoundException::new);

        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        return Response.ok(marshal(country, uriBuilder)).build();
    }

    private CountryResponse marshal(Country country, UriBuilder uriBuilder) {
        return new CountryResponse()
            .id(country.getId())
            .name(country.getName())
            .flagUrl(uriBuilder.path(country.getFlagUrl()).build());
    }
}
