package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategoryGroup;
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
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/rails")
@RolesAllowed("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Slf4j
public class CategoryResource {
    private final CategoryService categoryService;

    @GET
    @Path("/category-groups")
    public Response getCategoryGroups(@Context SecurityContext ctx,
                                      @Context UriInfo uriInfo,
                                      @QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting category groups [userId: {}]", userId);

        Page<CategoryGroup> groups = categoryService.getCategoryGroups(userId, page, pageSize);

        PaginatedCategoryGroups response = new PaginatedCategoryGroups()
            .page(groups.getPageIndex())
            .pageSize(groups.getPageSize())
            .count(groups.getContentSize())
            .total(groups.getTotalCount())
            .totalPages(groups.getTotalPages())
            .items(groups.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, groups));

        if (log.isDebugEnabled()) {
            log.debug("Listing category groups [userId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, page, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/category-groups/{groupId}")
    public Response getCategoryGroup(@Context SecurityContext ctx,
                                     @PathParam("groupId") UUID groupId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting category group [userId: {}, groupId: {}]", userId, groupId);

        CategoryGroup group = categoryService.getCategoryGroup(userId, groupId);
        return Response.ok(marshal(group)).build();
    }

    @POST
    @Path("/category-groups")
    public Response createCategoryGroup(@Context SecurityContext ctx,
                                        @Context UriInfo uriInfo,
                                        CategoryGroupRequest newGroup) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Creating category group [userId: {}, group: {}]", userId, newGroup.getName());

        CategoryGroup group = categoryService.createCategoryGroup(userId,
            newGroup.getName(), newGroup.getDescription());

        URI location = uriInfo.getBaseUriBuilder()
            .path(CategoryResource.class)
            .path(CategoryResource.class, "getCategoryGroup")
            .buildFromMap(Map.of("groupId", group.getId()));
        return Response.created(location).build();
    }

    @PUT
    @Path("/category-groups/{groupId}")
    public Response updateCategoryGroup(@Context SecurityContext ctx,
                                        @PathParam("groupId") UUID groupId,
                                        CategoryGroupRequest details) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating category group [userId: {}, groupId: {}, group: {}]",
            userId, groupId, details.getName());

        categoryService.updateCategoryGroup(userId, groupId,
            details.getName(), details.getDescription());

        return Response.noContent().build();
    }

    @DELETE
    @Path("/category-groups/{groupId}")
    public Response deleteCategoryGroup(@Context SecurityContext ctx,
                                        @PathParam("groupId") UUID groupId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting category group [userId: {}, groupId: {}]", userId, groupId);

        categoryService.deleteCategoryGroup(userId, groupId);
        return Response.noContent().build();
    }

    @GET
    @Path("/category-groups/{groupId}/categories")
    public Response getCategories(@Context SecurityContext ctx,
                                  @Context UriInfo uriInfo,
                                  @PathParam("groupId") UUID groupId,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("page-size") @DefaultValue("20") int pageSize) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting categories [userId: {}, groupId: {}]", userId, groupId);

        Page<Category> categories = categoryService.getCategories(userId, groupId, page, pageSize);

        PaginatedCategories response = new PaginatedCategories()
            .page(categories.getPageIndex())
            .pageSize(categories.getPageSize())
            .count(categories.getContentSize())
            .total(categories.getTotalCount())
            .totalPages(categories.getTotalPages())
            .items(categories.getContent().stream().map(this::marshal).toList())
            .links(PaginationUtils.buildPageLinks(uriInfo, categories));

        if (log.isDebugEnabled()) {
            log.debug("Listing categories [userId: {}, groupId: {}, page: {}, pageSize: {}, count: {}, total: {}]",
                userId, groupId, page, pageSize, response.getCount(), response.getTotal());
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/category-groups/{groupId}/categories")
    public Response createCategory(@Context SecurityContext ctx,
                                   @Context UriInfo uriInfo,
                                   @PathParam("groupId") UUID groupId,
                                   CategoryRequest newCategory) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Creating category [userId: {}, groupId: {}, category: {}]",
            userId, groupId, newCategory.getName());

        Category category = categoryService.createCategory(userId, groupId,
            newCategory.getName(), newCategory.getDescription(), newCategory.getColour());

        URI location = uriInfo.getBaseUriBuilder()
            .path(CategoryResource.class)
            .path(CategoryResource.class, "getCategory")
            .buildFromMap(Map.of("categoryId", category.getId()));
        return Response.created(location).build();
    }

    @GET
    @Path("/categories/{categoryId}")
    public Response getCategory(@Context SecurityContext ctx,
                                @PathParam("categoryId") UUID categoryId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Get category [userId: {}, category: {}]", userId, categoryId);

        Category category = categoryService.getCategory(userId, categoryId);
        return Response.ok(marshal(category)).build();
    }

    @PUT
    @Path("/categories/{categoryId}")
    public Response updateCategory(@Context SecurityContext ctx,
                                   @PathParam("categoryId") UUID categoryId,
                                   CategoryRequest details) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Updating category [userId: {}, categoryId: {}, category: {}]", userId, categoryId, details.getName());

        categoryService.updateCategory(userId, categoryId,
            details.getName(), details.getDescription(), details.getColour());

        return Response.noContent().build();
    }

    @DELETE
    @Path("/categories/{categoryId}")
    public Response deleteCategory(@Context SecurityContext ctx,
                                   @PathParam("categoryId") UUID categoryId) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Deleting category [userId: {}, categoryId: {}]", userId, categoryId);

        categoryService.deleteCategory(userId, categoryId);
        return Response.noContent().build();
    }

    @GET
    @Path("/categories/{categoryId}/selectors/{accountId}")
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
    @Path("/categories/{categoryId}/selectors/{accountId}")
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

        Collection<AccountCategorySelector> result =
            categoryService.setCategorySelectors(userId, categoryId, accountId, newSelectors).stream()
                .map(this::marshal)
                .toList();

        return Response.ok(result).build();
    }

    @GET
    @Path("/category-groups/{groupId}/statistics")
    public Response getCategoryStatistics(@Context SecurityContext ctx,
                                          @PathParam("groupId") UUID groupId,
                                          @QueryParam("from-date") LocalDate fromDate,
                                          @QueryParam("to-date") LocalDate toDate) {
        UUID userId = AuthUtils.getUserId(ctx);
        log.info("Getting category statistics [userId: {}, groupId: {}, fromDate: {}, toDate: {}]",
            userId, groupId, fromDate, toDate);

        // convert dates to instant
        Instant startDate = (fromDate == null)
            ? Instant.EPOCH.truncatedTo(ChronoUnit.DAYS)
            : fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = (toDate == null)
            ? Instant.now().truncatedTo(ChronoUnit.DAYS)
            : toDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CategoryStatisticsResponse> result = categoryService.getStatistics(userId, groupId, startDate, endDate)
            .stream()
            .map(this::marshal)
            .toList();
        return Response.ok(result).build();
    }

    private CategoryGroupResponse marshal(CategoryGroup group) {
        return new CategoryGroupResponse()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription());
    }

    private CategoryResponse marshal(Category category) {
        return new CategoryResponse()
            .id(category.getId())
            .groupId(category.getGroup().getId())
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
            .description(statistics.getDescription())
            .colour(statistics.getColour())
            .count(statistics.getCount())
            .total(statistics.getTotal().doubleValue())
            .credit(statistics.getCredit().doubleValue())
            .debit(statistics.getDebit().doubleValue());
    }
}
