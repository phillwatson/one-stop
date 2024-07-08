package com.hillayes.rail.resource;

import com.hillayes.auth.jwt.AuthUtils;
import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.service.CategoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
}
