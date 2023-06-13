package com.hillayes.rail.resource;

import com.hillayes.onestop.api.CountryResponse;
import com.hillayes.onestop.api.PaginatedCountries;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.domain.Country;
import com.hillayes.rail.service.CountryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
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

        // reduce the overall list to the subset identified by the page parameters
        List<CountryResponse> countryPage = allCountries.stream()
            .sorted(Comparator.comparing(Country::getName))
            .skip(((long) page) * pageSize)
            .limit(pageSize)
            .map(country -> marshal(country, uriInfo.getAbsolutePathBuilder().path(country.getId())))
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
        Country country = countryService.get(id).orElseThrow(() -> new NotFoundException("country", id));

        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        return Response.ok(marshal(country, uriBuilder)).build();
    }

    @GET
    @Path("/{id}/logos")
    @Produces("image/png")
    public Response getCountryLogo(@Context UriInfo uriInfo,
                                   @PathParam("id") String id) {
        log.info("Get country logo [id: {}]", id);

        StreamingOutput content = output -> {
            try (output) {
                try (InputStream resource = countryService.getLogo(id)
                    .orElseThrow(javax.ws.rs.NotFoundException::new)) {
                    IOUtils.copy(resource, output);
                }
                output.flush();
            }
        };

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(true);
        cacheControl.setMaxAge((int)Duration.ofDays(365).toSeconds());
        cacheControl.setSMaxAge((int)Duration.ofDays(365).toSeconds());

        return Response.ok(content)
            .cacheControl(cacheControl)
            .header("Content-Disposition", "attachment; filename=\"" + id + ".png\"")
            .build();
    }

    private CountryResponse marshal(Country country, UriBuilder uriBuilder) {
        CountryResponse result = new CountryResponse()
            .id(country.getId())
            .name(country.getName());

        if (country.getFlagUri() != null) {
            result.flagUri(uriBuilder
                .path("logos")
                .queryParam("version", 1)
                .build());
        }

        return result;
    }
}
