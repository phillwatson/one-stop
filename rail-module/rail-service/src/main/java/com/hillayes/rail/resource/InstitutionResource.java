package com.hillayes.rail.resource;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedInstitutions;
import com.hillayes.onestop.api.PaginationUtils;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.service.InstitutionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                           @QueryParam("rail") RailProvider railProvider,
                           @QueryParam("country") @DefaultValue("GB") String countryCode) {
        log.info("List institutions [country: {}, page: {}, pageSize: {}]", countryCode, page, pageSize);
        Set<RailInstitution> allBanks = new HashSet<>(institutionService.list(railProvider, countryCode, true));
        allBanks.addAll(institutionService.list(railProvider, countryCode, false));

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
            .links(PaginationUtils.buildPageLinks(uriInfo, page, pageSize, totalPages));

        log.debug("List institutions [country: {}, page: {}, pageSize: {}, count: {}, total: {}]",
            countryCode, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        log.info("Get institution [id: {}]", id);
        RailInstitution institution = institutionService.get(id)
            .orElseThrow(() -> new NotFoundException("Institution", id));

        return Response.ok(marshal(institution)).build();
    }

    private InstitutionResponse marshal(RailInstitution institution) {
        return new InstitutionResponse()
            .id(institution.getId())
            .provider(institution.getProvider().name())
            .name(institution.getName())
            .bic(institution.getBic())
            .logo(institution.getLogo());
    }
}
