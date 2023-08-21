package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedInstitutions;
import com.hillayes.rail.model.Institution;
import com.hillayes.rail.model.InstitutionDetail;
import com.hillayes.rail.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path("/api/v1/rails/institutions")
@RolesAllowed({"admin", "user"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class InstitutionResource {
    private final InstitutionService institutionService;

    @GET
    public Response getAll(@Context UriInfo uriInfo,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("page-size") @DefaultValue("20") int pageSize,
                           @QueryParam("country") String countryCode) {
        log.info("List institutions [country: {}, page: {}, pageSize: {}]", countryCode, page, pageSize);
        Set<Institution> allBanks = new HashSet<>(institutionService.list(countryCode, true));
        allBanks.addAll(institutionService.list(countryCode, false));

        // reduce the overall list to the subset identified by the page parameters
        List<InstitutionResponse> bankPage = allBanks.stream()
            .sorted()
            .skip(((long) page) * pageSize)
            .limit(pageSize)
            .map(this::marshal)
            .toList();

        // convert the subset to a paginated response
        int totalPages = (int) Math.ceil((double) allBanks.size() / pageSize);
        PaginatedInstitutions response = new PaginatedInstitutions()
            .page(page)
            .pageSize(pageSize)
            .count(bankPage.size())
            .total((long) allBanks.size())
            .items(bankPage)
            .links(ResourceUtils.buildPageLinks(uriInfo, page, pageSize, totalPages,
                uriBuilder -> {
                    if (countryCode != null) {
                        uriBuilder.queryParam("country", countryCode);
                    }
                    return uriBuilder;
                })
            );

        log.debug("List institutions [country: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            countryCode, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        log.info("Get institution [id: {}]", id);
        InstitutionDetail institution = institutionService.get(id)
            .orElseThrow(() -> new NotFoundException("Institution", id));

        return Response.ok(marshal(institution)).build();
    }

    private InstitutionResponse marshal(Institution institution) {
        return new InstitutionResponse()
            .id(institution.id)
            .name(institution.name)
            .bic(institution.bic)
            .logo(institution.logo);
    }

    private InstitutionResponse marshal(InstitutionDetail institution) {
        return new InstitutionResponse()
            .id(institution.id)
            .name(institution.name)
            .bic(institution.bic)
            .logo(institution.logo);
    }
}
