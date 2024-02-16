package com.hillayes.rail.resource;

import com.hillayes.onestop.api.CountryResponse;
import com.hillayes.onestop.api.PaginatedCountries;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.PaginationUtils;
import com.hillayes.rail.domain.Country;
import com.hillayes.rail.service.CountryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
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
                           @QueryParam("page") @DefaultValue("0") int pageIndex,
                           @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        log.info("List countries");

        Collection<Country> allCountries = countryService.list();

        // reduce the overall list to the subset identified by the page parameters
        List<CountryResponse> countryPage = allCountries.stream()
            .sorted(Comparator.comparing(Country::getName))
            .skip(((long) pageIndex) * pageSize)
            .limit(pageSize)
            .map(country -> marshal(country, uriInfo.getAbsolutePathBuilder().path(country.getId())))
            .toList();

        // convert the subset to a paginated response
        int totalPages = (int) Math.ceil((double) allCountries.size() / pageSize);
        PaginatedCountries response = new PaginatedCountries()
            .page(pageIndex)
            .pageSize(pageSize)
            .count(countryPage.size())
            .total((long) allCountries.size())
            .items(countryPage)
            .links(PaginationUtils.buildPageLinks(uriInfo, pageIndex, pageSize, totalPages));

        log.debug("List countries [page: {}, pageSize: {}, count: {}, total: {}]",
            pageIndex, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{countryId}")
    public Response getById(@Context UriInfo uriInfo,
                            @PathParam("countryId") String countryId) {
        log.info("Get country [countryId: {}]", countryId);
        Country country = countryService.get(countryId)
            .orElseThrow(() -> new NotFoundException("Country", countryId));

        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        return Response.ok(marshal(country, uriBuilder)).build();
    }

    @GET
    @Path("/{countryId}/logos")
    @Produces("image/png")
    public Response getCountryLogo(@Context UriInfo uriInfo,
                                   @PathParam("countryId") String countryId) {
        log.info("Get country logo [countryId: {}]", countryId);

        StreamingOutput content = output -> {
            try (output) {
                try (InputStream resource = countryService.getLogo(countryId)
                    .orElseThrow(jakarta.ws.rs.NotFoundException::new)) {
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
            .header("Content-Disposition", "attachment; filename=\"" + countryId + ".png\"")
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
