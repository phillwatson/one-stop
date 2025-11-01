package com.hillayes.integration.api.rail;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class CategoryApi extends ApiBase {
    private static final TypeRef<List<AccountCategorySelector>> SELECTORS_LIST = new TypeRef<>() {};

    public CategoryApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public PaginatedCategoryGroups getCategoryGroups(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/category-groups")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedCategoryGroups.class);
    }

    public CategoryGroupResponse getCategoryGroup(UUID groupId) {
        return getCategoryGroup(groupId, 200)
            .as(CategoryGroupResponse.class);
    }

    public Response getCategoryGroup(UUID groupId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/category-groups/{groupId}", groupId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public CategoryGroupResponse createCategoryGroup(CategoryGroupRequest request) {
        return createCategoryGroup(request, 201)
            .as(CategoryGroupResponse.class);
    }

    public Response createCategoryGroup(CategoryGroupRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/rails/category-groups")
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public CategoryGroupResponse updateCategoryGroup(UUID groupId, CategoryGroupRequest request) {
        return updateCategoryGroup(groupId, request, 200)
            .as(CategoryGroupResponse.class);
    }

    public Response updateCategoryGroup(UUID groupId, CategoryGroupRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/rails/category-groups/{groupId}", groupId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public Response deleteCategoryGroup(UUID groupId) {
        return deleteCategoryGroup(groupId, 204);
    }

    public Response deleteCategoryGroup(UUID groupId, int expectedStatus) {
        return givenAuth()
            .delete("/api/v1/rails/category-groups/{groupId}", groupId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }

    public PaginatedCategories getCategories(UUID groupId, int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/rails/category-groups/{groupId}/categories", groupId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedCategories.class);
    }

    public CategoryResponse getCategory(UUID categoryId) {
        return getCategory(categoryId, 200)
            .as(CategoryResponse.class);
    }

    public Response getCategory(UUID categoryId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/categories/{categoryId}", categoryId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public CategoryResponse createCategory(UUID groupId, CategoryRequest request) {
        return createCategory(groupId, request, 201)
            .as(CategoryResponse.class);
    }

    public Response createCategory(UUID groupId, CategoryRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/rails/category-groups/{groupId}/categories", groupId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) {
        return updateCategory(categoryId, request, 200)
            .as(CategoryResponse.class);
    }

    public Response updateCategory(UUID categoryId, CategoryRequest request, int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/rails/categories/{categoryId}", categoryId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public Response deleteCategory(UUID categoryId) {
        return deleteCategory(categoryId, 204);
    }

    public Response deleteCategory(UUID categoryId, int expectedStatus) {
        return givenAuth()
            .delete("/api/v1/rails/categories/{categoryId}", categoryId)
            .then()
            .statusCode(expectedStatus)
            .extract().response();
    }

    public List<AccountCategorySelector> getAccountCategorySelectors(UUID categoryId, UUID accountId) {
        return getAccountCategorySelectors(categoryId, accountId, 200)
            .as(SELECTORS_LIST);
    }

    public Response getAccountCategorySelectors(UUID categoryId, UUID accountId, int expectedStatus) {
        return givenAuth()
            .get("/api/v1/rails/categories/{categoryId}/selectors/{accountId}", categoryId, accountId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }

    public List<AccountCategorySelector> setAccountCategorySelectors(UUID categoryId, UUID accountId,
                                                               Collection<AccountCategorySelector> selectors) {
        return setAccountCategorySelectors(categoryId, accountId, selectors, 200)
            .as(SELECTORS_LIST);
    }

    public Response setAccountCategorySelectors(UUID categoryId, UUID accountId,
                                                Collection<AccountCategorySelector> selectors,
                                                int expectedStatus) {
        return givenAuth()
            .contentType(JSON)
            .body(selectors)
            .put("/api/v1/rails/categories/{categoryId}/selectors/{accountId}", categoryId, accountId)
            .then()
            .statusCode(expectedStatus)
            .contentType(JSON)
            .extract().response();
    }
}
