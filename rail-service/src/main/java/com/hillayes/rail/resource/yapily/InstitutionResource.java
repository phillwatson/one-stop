package com.hillayes.rail.resource.yapily;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.onestop.api.InstitutionResponse;
import com.hillayes.onestop.api.PaginatedInstitutions;
import com.hillayes.rail.resource.ResourceUtils;
import com.hillayes.rail.service.yapily.InstitutionService;
import com.hillayes.yapily.model.Institution;
import com.hillayes.yapily.model.Media;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Path("/api/v1/rails/yapily/institutions")
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
        List<Institution> institutions = institutionService.list(countryCode, true);

        // reduce the overall list to the subset identified by the page parameters
        List<InstitutionResponse> bankPage = institutions.stream()
            .sorted(Comparator.comparing(Institution::getFullName))
            .skip(((long) page) * pageSize)
            .limit(pageSize)
            .map(this::marshal)
            .toList();

        // convert the subset to a paginated response
        int totalPages = (int) Math.ceil((double) institutions.size() / pageSize);
        PaginatedInstitutions response = new PaginatedInstitutions()
            .page(page)
            .pageSize(pageSize)
            .count(bankPage.size())
            .total((long) institutions.size())
            .items(bankPage)
            .links(ResourceUtils.buildPageLinks(uriInfo, page, pageSize, totalPages));

        log.debug("List institutions [page: {}, pageSize: {}, count: {}, total: {}]",
            page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        log.info("Get institution [id: {}]", id);
        Institution institution = institutionService.get(id)
            .orElseThrow(() -> new NotFoundException("Institution", id));

        return Response.ok(marshal(institution)).build();
    }

    private InstitutionResponse marshal(Institution institution) {
        String logo = institution.getMedia() == null ? null : institution.getMedia().stream()
            .filter(m -> "icon".equals(m.getType()))
            .map(Media::getSource)
            .filter(Objects::nonNull)
            .findAny()
            .or(() -> institution.getMedia().stream()
                .filter(m -> "logo".equals(m.getType()))
                .map(Media::getSource)
                .filter(Objects::nonNull)
                .findAny()
            ).orElse(null);

        return new InstitutionResponse()
            .id(institution.getId())
            .name(institution.getFullName())
            .bic("")
            .logo(logo);
    }
}
