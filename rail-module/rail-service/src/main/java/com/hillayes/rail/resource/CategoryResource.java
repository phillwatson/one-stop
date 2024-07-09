package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.domain.CategoryStatistics;
import com.hillayes.rail.service.CategoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/rails/categories")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class CategoryResource {
    private final CategoryService categoryService;

    @GET
    public Response getCategories(@Context SecurityContext ctx,
                                  @Context UriInfo uriInfo,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting categories [userId: {}]", userId);

        Page<Category> categories = categoryService.getCategories(userId, page, pageSize);

        PaginatedCategories response = new PaginatedCategories()
            .page(categories.getPageIndex())
            .pageSize(categories.getPageSize())
            .count(categories.getContentSize())
            .total(categories.getTotalCount())
            .totalPages(categories.getTotalPages())
            .items(categories.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, categories));

        log.debug("Listing categories [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, page, pageSize, response.getCount(), response.getTotal());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{categoryId}")
    public Response getCategory(@Context SecurityContext ctx,
                                @PathParam("categoryId") UUID categoryId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Get category [userId: {}, category: {}]", userId, categoryId);

        CategoryResponse category = marshal(categoryService.getCategory(userId, categoryId));
        return Response.ok(category).build();
    }

    @POST
    public Response createCategory(@Context SecurityContext ctx,
                                   @Context UriInfo uriInfo,
                                   CategoryRequest newCategory) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Creating category [userId: {}, category: {}]", userId, newCategory.getName());

        Category category = categoryService.createCategory(AuthUtils.getUserId(ctx),
            newCategory.getName(), newCategory.getDescription(), newCategory.getColour());

        URI location = uriInfo.getBaseUriBuilder()
            .path(CategoryResource.class)
            .path(category.getId().toString())
            .build();
        return Response.created(location).build();
    }

    @PUT
    @Path("/{categoryId}")
    public Response updateCategory(@Context SecurityContext ctx,
                                   @PathParam("categoryId") UUID categoryId,
                                   CategoryRequest details) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating category [userId: {}, categoryId: {}, category: {}]", userId, categoryId, details.getName());

        categoryService.updateCategory(AuthUtils.getUserId(ctx), categoryId,
            details.getName(), details.getDescription(), details.getColour());

        return Response.noContent().build();
    }

    @GET
    @Path("/{categoryId}/selectors/{accountId}")
    public Response getCategorySelectors(@Context SecurityContext ctx,
                                         @PathParam("categoryId") UUID categoryId,
                                         @PathParam("accountId") UUID accountId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting category selectors [userId: {}, categoryId: {}, accountId: {}]", userId, categoryId, accountId);

        Collection<AccountCategorySelector> result =
            categoryService.getCategorySelectors(userId, categoryId, accountId).stream()
            .map(this::marshal)
            .toList();

        log.debug("Getting category selectors [userId: {}, categoryId: {}, accountId: {}, count: {}]",
            userId, categoryId, accountId, result.size());
        return Response.ok(result).build();
    }

    @PUT
    @Path("/{categoryId}/selectors/{accountId}")
    public Response setCategorySelectors(@Context SecurityContext ctx,
                                         @PathParam("categoryId") UUID categoryId,
                                         @PathParam("accountId") UUID accountId,
                                         List<AccountCategorySelector> selectors) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Setting category selectors [userId: {}, categoryId: {}, accountId: {}]", userId, categoryId, accountId);

        List<CategorySelector> newSelectors = selectors.stream()
            .map(selector -> CategorySelector.builder()
                .creditorContains(selector.getCreditorContains())
                .refContains(selector.getRefContains())
                .infoContains(selector.getInfoContains())
                .build())
            .toList();
        categoryService.setCategorySelectors(userId, categoryId, accountId, newSelectors);

        return Response.noContent().build();
    }

    @GET
    @Path("/statistics")
    public Response getCategoryStatistics(@Context SecurityContext ctx,
                                          @QueryParam("from-date") LocalDate fromDate,
                                          @QueryParam("to-date") LocalDate toDate) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting category statistics [userId: {}, fromDate: {}, toDate: {}]", userId, fromDate, toDate);

        // convert dates to instant
        Instant startDate = (fromDate == null)
            ? Instant.EPOCH.truncatedTo(ChronoUnit.DAYS)
            : fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = (toDate == null)
            ? Instant.now().truncatedTo(ChronoUnit.DAYS)
            : toDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CategoryStatisticsResponse> result = categoryService.getStatistics(userId, startDate, endDate).stream()
            .map(this::marshal)
            .toList();
        return Response.ok(result).build();
    }

    private CategoryResponse marshal(Category category) {
        return new CategoryResponse()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .colour(category.getColour());
    }

    private AccountCategorySelector marshal(CategorySelector selector) {
        return new AccountCategorySelector()
            .creditorContains(selector.getCreditorContains())
            .refContains(selector.getRefContains())
            .infoContains(selector.getInfoContains());
    }

    private CategoryStatisticsResponse marshal(CategoryStatistics statistics) {
        return new CategoryStatisticsResponse()
            .category(statistics.getCategory())
            .categoryId(statistics.getCategoryId())
            .count(statistics.getCount())
            .total(statistics.getTotal().doubleValue());
    }
}
