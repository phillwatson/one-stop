package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategoryGroup;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.domain.CategoryStatistics;
import com.hillayes.rail.service.CategoryService;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CategoryResourceTest extends TestBase {
    private static final TypeRef<Collection<CategorySelectorResponse>> ACCOUNT_CATEGORY_SELECTOR_LIST = new TypeRef<>() {};
    private static final TypeRef<List<CategoryStatisticsResponse>> CATEGORY_STATISTICS_RESPONSE_LIST = new TypeRef<>() {};

    @InjectMock
    CategoryService categoryService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategoryGroups() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a page range
        int page = 2;
        int pageSize = 19;

        // and: a paginated list of category groups
        Page<CategoryGroup> groups = Page.of(mockCategoryGroups(userId, 40), page, pageSize);
        when(categoryService.getCategoryGroups(userId, page, pageSize))
            .thenReturn(groups);

        // when: a paginated list of category groups is requested
        PaginatedCategoryGroups response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/category-groups")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCategoryGroups.class);

        // then: the category-service is called with the authenticated user-id and page
        verify(categoryService).getCategoryGroups(userId, page, pageSize);

        // and: the response contains the paginated list of category groups
        assertEquals(page, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(groups.getContentSize(), response.getCount());
        assertEquals(groups.getTotalCount(), response.getTotal());
        assertEquals(groups.getTotalPages(), response.getTotalPages());

        // and: the page links contain given filter properties
        PageLinks links = response.getLinks();
        assertTrue(links.getFirst().getQuery().contains("page=0"));
        assertTrue(links.getFirst().getQuery().contains("page-size=" + pageSize));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: an existing category group
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // when: a category group is requested
        CategoryGroupResponse response = given()
            .request()
            .pathParam("groupId", group.getId())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/category-groups/{groupId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(CategoryGroupResponse.class);

        // then: the response contains the requested category group
        assertEquals(group.getId(), response.getId());
        assertEquals(group.getName(), response.getName());
        assertEquals(group.getDescription(), response.getDescription());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreateCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a new category group request
        CategoryGroupRequest request = new CategoryGroupRequest()
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20));

        // and: the service is mocked to return the new category group
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription()).build();
        when(categoryService.createCategoryGroup(userId, request.getName(), request.getDescription()))
            .thenReturn(group);

        // when: a new category is created
        Response response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/rails/category-groups")
            .then()
            .statusCode(201)
            .extract().response();

        // then: the category-service is called with the authenticated user-id and new category group details
        verify(categoryService).createCategoryGroup(userId, request.getName(), request.getDescription());

        // and: the new category group locator is returned
        String location = response.header("Location");
        assertTrue(location.contains("/api/v1/rails/category-groups/" + group.getId().toString()));

        // and: the response contains the new category group
        CategoryGroupResponse responseBody = response.as(CategoryGroupResponse.class);
        assertEquals(group.getId(), responseBody.getId());
        assertEquals(group.getName(), responseBody.getName());
        assertEquals(group.getDescription(), responseBody.getDescription());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group update request
        CategoryGroupRequest request = new CategoryGroupRequest()
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20));

        // and: a category group to be updated
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription()).build();
        when(categoryService.updateCategoryGroup(userId, group.getId(), request.getName(), request.getDescription()))
            .thenReturn(group);

        // when: the category is updated
        CategoryGroupResponse response = given()
            .request()
            .pathParam("groupId", group.getId())
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/category-groups/{groupId}")
            .then()
            .statusCode(200)
            .extract().as(CategoryGroupResponse.class);

        // then: the category-service is called with the authenticated user-id and updated category group details
        verify(categoryService).updateCategoryGroup(userId, group.getId(),
            request.getName(), request.getDescription());

        // and: the response contains the updated category group
        assertEquals(group.getId(), response.getId());
        assertEquals(group.getName(), response.getName());
        assertEquals(group.getDescription(), response.getDescription());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group belonging to the user
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20)).build();
        when(categoryService.deleteCategoryGroup(userId, group.getId()))
            .thenReturn(group);

        // when: the category is deleted
        given()
            .request()
            .pathParam("groupId", group.getId())
            .contentType(JSON)
            .when()
            .delete("/api/v1/rails/category-groups/{groupId}")
            .then()
            .statusCode(204);

        // then: the category-service is called with the authenticated user-id and group id
        verify(categoryService).deleteCategoryGroup(userId, group.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategories() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group belonging to the user
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // and: a page range
        int page = 2;
        int pageSize = 19;

        // and: a paginated list of categories
        Page<Category> categories = Page.of(mockCategories(group, 40), page, pageSize);
        when(categoryService.getCategories(userId, group.getId(), page, pageSize))
            .thenReturn(categories);

        // when: a paginated list of categories is requested
        PaginatedCategories response = given()
            .request()
            .pathParam("groupId", group.getId())
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/category-groups/{groupId}/categories")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCategories.class);

        // then: the category-service is called with the authenticated user-id, group-id and page
        verify(categoryService).getCategories(userId, group.getId(), page, pageSize);

        // and: the response contains the paginated list of categories
        assertEquals(page, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(categories.getContentSize(), response.getCount());
        assertEquals(categories.getTotalCount(), response.getTotal());
        assertEquals(categories.getTotalPages(), response.getTotalPages());

        // and: the page links contain given filter properties
        PageLinks links = response.getLinks();
        assertTrue(links.getFirst().getQuery().contains("page=0"));
        assertTrue(links.getFirst().getQuery().contains("page-size=" + pageSize));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group belonging to the user
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // and: an existing category
        Category category = mockCategory(group, insecure().nextAlphanumeric(20));

        // when: a category is requested
        CategoryResponse response = given()
            .request()
            .pathParam("categoryId", category.getId())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/categories/{categoryId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(CategoryResponse.class);

        // then: the response contains the requested category
        assertEquals(category.getId(), response.getId());
        assertEquals(category.getGroup().getId(), response.getGroupId());
        assertEquals(category.getName(), response.getName());
        assertEquals(category.getDescription(), response.getDescription());
        assertEquals(category.getColour(), response.getColour());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreateCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // and: a new category request
        CategoryRequest request = new CategoryRequest()
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20))
            .colour(insecure().nextAlphanumeric(20));

        // and: the service is mocked to return the new category
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .group(group)
            .name(request.getName())
            .description(request.getDescription())
            .colour(request.getColour()).build();
        when(categoryService.createCategory(userId, group.getId(), request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: a new category is created
        Response response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .pathParam("groupId", group.getId())
            .when()
            .post("/api/v1/rails/category-groups/{groupId}/categories")
            .then()
            .statusCode(201)
            .extract().response();

        // then: the category-service is called with the authenticated user-id, group-id and new category details
        verify(categoryService).createCategory(userId, group.getId(), request.getName(), request.getDescription(), request.getColour());

        // and: the new category locator is returned
        String location = response.header("Location");
        assertTrue(location.contains("/api/v1/rails/categories/" + category.getId().toString()));

        // and: the response contains the new category
        CategoryResponse responseBody = response.as(CategoryResponse.class);
        assertEquals(category.getId(), responseBody.getId());
        assertEquals(category.getGroup().getId(), responseBody.getGroupId());
        assertEquals(category.getName(), responseBody.getName());
        assertEquals(category.getDescription(), responseBody.getDescription());
        assertEquals(category.getColour(), responseBody.getColour());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: the user has a category group
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // and: a category update request
        CategoryRequest request = new CategoryRequest()
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20))
            .colour(insecure().nextAlphanumeric(20));

        // and: a category to be updated
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .group(group)
            .name(request.getName())
            .description(request.getDescription())
            .colour(request.getColour()).build();
        when(categoryService.updateCategory(userId, category.getId(), request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: the category is updated
        CategoryResponse response = given()
            .request()
            .pathParam("categoryId", category.getId())
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/categories/{categoryId}")
            .then()
            .statusCode(200)
            .extract().as(CategoryResponse.class);

        // then: the category-service is called with the authenticated user-id and updated category details
        verify(categoryService).updateCategory(userId, category.getId(),
            request.getName(), request.getDescription(), request.getColour());

        // and: the response contains the updated category
        assertEquals(category.getId(), response.getId());
        assertEquals(category.getGroup().getId(), response.getGroupId());
        assertEquals(category.getName(), response.getName());
        assertEquals(category.getDescription(), response.getDescription());
        assertEquals(category.getColour(), response.getColour());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category belonging to the user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name(insecure().nextAlphanumeric(20))
            .description(insecure().nextAlphanumeric(20)).build();
        when(categoryService.deleteCategory(userId, category.getId()))
            .thenReturn(category);

        // when: the category is deleted
        given()
            .request()
            .pathParam("categoryId", category.getId())
            .contentType(JSON)
            .when()
            .delete("/api/v1/rails/categories/{categoryId}")
            .then()
            .statusCode(204);

        // then: the category-service is called with the authenticated user-id and category id
        verify(categoryService).deleteCategory(userId, category.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategorySelectors() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category-id and account-id
        UUID categoryId = UUID.randomUUID();

        // and: a list of category-selectors - across a range of account IDs
        Collection<CategorySelector> categorySelectors = IntStream.range(0, 4)
            .mapToObj(i -> UUID.randomUUID())
            .map(accountId -> mockCategorySelectors(5, accountId, categoryId))
            .flatMap(Collection::stream)
            .toList();
        when(categoryService.getCategorySelectors(eq(userId), eq(categoryId), anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(2);
            int pagesize = invocation.getArgument(3);
            return Page.of(categorySelectors, pageIndex, pagesize);
        });

        // when: the category selectors are requested
        PaginatedCategorySelectors response = given()
            .request()
            .pathParam("categoryId", categoryId)
            .queryParam("page", 0)
            .queryParam("page-size", 100) // more than available selectors
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/categories/{categoryId}/selectors")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCategorySelectors.class);

        // then: the category-service is called with the authenticated user-id, category-id
        verify(categoryService).getCategorySelectors(userId, categoryId, 0, 100);

        // and: the response contains the list of category-selectors
        assertEquals(categorySelectors.size(), response.getCount());

        // and: the selectors cover all accounts
        assertNotNull(response.getItems());
        categorySelectors.forEach(expected -> {
            CategorySelectorResponse actual = response.getItems().stream()
                .filter(selector -> selector.getId().equals(expected.getId()))
                .findFirst()
                .orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getAccountId(), actual.getAccountId());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCategorySelectorsForAccount() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category-id and account-id
        UUID categoryId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        // and: a list of category-selectors
        Collection<CategorySelector> categorySelectors = mockCategorySelectors(5, accountId, categoryId);
        when(categoryService.getCategorySelectors(eq(userId), eq(categoryId), eq(accountId), anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(3);
            int pageSize = invocation.getArgument(4);
            return Page.of(categorySelectors, pageIndex, pageSize);
        });

        // when: the category selectors are requested
        PaginatedCategorySelectors response = given()
            .request()
            .pathParam("categoryId", categoryId)
            .pathParam("accountId", accountId)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/categories/{categoryId}/account-selectors/{accountId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCategorySelectors.class);

        // then: the category-service is called with the authenticated user-id, category-id, and account-id
        verify(categoryService).getCategorySelectors(userId, categoryId, accountId, 0, 20);

        // and: the response contains the list of category-selectors
        assertEquals(categorySelectors.size(), response.getCount());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testSetCategorySelectors() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category-id and account-id
        UUID categoryId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        // and: a collection of new category-selectors
        List<CategorySelectorRequest> selectors = List.of(
            new CategorySelectorRequest().infoContains(insecure().nextAlphanumeric(20)),
            new CategorySelectorRequest().refContains(insecure().nextAlphanumeric(20)),
            new CategorySelectorRequest().creditorContains(insecure().nextAlphanumeric(20))
        );

        // and: the service is primed with data
        List<CategorySelector> updatedSelectors = mockCategorySelectors(selectors.size(), accountId, categoryId);
        when(categoryService.setCategorySelectors(eq(userId), eq(categoryId), eq(accountId), anyList()))
            .thenReturn(updatedSelectors);

        // when: the category selectors are updated
        Collection<CategorySelectorResponse> response = given()
            .request()
            .pathParam("categoryId", categoryId)
            .pathParam("accountId", accountId)
            .contentType(JSON)
            .body(selectors)
            .when()
            .put("/api/v1/rails/categories/{categoryId}/account-selectors/{accountId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(ACCOUNT_CATEGORY_SELECTOR_LIST);

        // then: the response contains the list of updated category-selectors
        assertEquals(updatedSelectors.size(), response.size());

        // and: the category-service is called with the authenticated user-id, category-id, account-id, and new category-selectors
        ArgumentCaptor<List<CategorySelector>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryService).setCategorySelectors(eq(userId), eq(categoryId), eq(accountId), captor.capture());

        // and: the new category-selectors are passed to the category-service
        List<CategorySelector> actual = captor.getValue();
        assertEquals(selectors.size(), actual.size());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testMoveCategorySelector() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group with categories exist
        CategoryGroup categoryGroup = mockCategoryGroup(userId, "mock-group");
        Category[] categories = new Category[] {
            mockCategory(categoryGroup, "mock-category-1"),
            mockCategory(categoryGroup, "mock-category-2")
        };

        // and: a collection of new category-selectors
        UUID accountId = UUID.randomUUID();
        CategorySelector selector = mockCategorySelector(accountId, categories[0]);

        // and: the service is primed with data
        when(categoryService.moveCategorySelector(
            eq(userId), eq(selector.getCategory().getId()), eq(selector.getId()), any(UUID.class)))
            .then(invocation -> {
                UUID destCategoryId = invocation.getArgument(3);
                selector.setCategory(Arrays.stream(categories)
                    .filter(c -> c.getId().equals(destCategoryId))
                    .findFirst().orElse(null)
                );
                return selector;
            });

        // when: a category selector is moved
        CategorySelectorUpdateRequest request = new CategorySelectorUpdateRequest()
            .categoryId(categories[1].getId());
        CategorySelectorResponse response = given()
            .request()
            .pathParam("categoryId", selector.getCategory().getId())
            .pathParam("selectorId", selector.getId())
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/categories/{categoryId}/selectors/{selectorId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(CategorySelectorResponse.class);

        // then: the service is called to move the identified selector to the identified category
        verify(categoryService).moveCategorySelector(
            userId, categories[0].getId(), selector.getId(), request.getCategoryId());

        // and: the updated category selector is returned
        assertEquals(selector.getId(), response.getId());
        assertEquals(request.getCategoryId(), response.getCategoryId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteCategorySelectors() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category-id and account-id
        CategoryGroup categoryGroup = mockCategoryGroup(userId, "mock-group");
        Category category = mockCategory(categoryGroup, "mock-category");

        // and: a collection of new category-selectors
        UUID accountId = UUID.randomUUID();
        CategorySelector selector = mockCategorySelector(accountId, category);

        // and: the service is primed with data
        when(categoryService.deleteCategorySelector(userId, selector.getCategory().getId(), selector.getId()))
            .thenReturn(selector);

        // when: a category selectors is deleted
        given()
            .request()
            .pathParam("categoryId", selector.getCategory().getId())
            .pathParam("selectorId", selector.getId())
            .contentType(JSON)
            .when()
            .delete("/api/v1/rails/categories/{categoryId}/selectors/{selectorId}")
            .then()
            .statusCode(204);

        // then: the service is called to delete the identified selector
        verify(categoryService).deleteCategorySelector(userId, category.getId(), selector.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetStatistics() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group ID
        CategoryGroup group = mockCategoryGroup(userId, insecure().nextAlphanumeric(20));

        // and: a date range
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();

        // and: the service is primed with data
        Instant startDate = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = toDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<CategoryStatistics> expectedResult = List.of(
            TestData.mockCategoryStatistics(group, "cat-1", 20, 123.44, 282.93,11.25),
            TestData.mockCategoryStatistics(group, "cat-2", 10, 456.44, 222.73,21.225),
            TestData.mockCategoryStatistics(group, "cat-3", 6, 34.44, 82.73,177.25)
        );
        when(categoryService.getStatistics(userId, group.getId(), startDate, endDate))
            .thenReturn(expectedResult);

        // when: the statistics are requested
        Collection<CategoryStatisticsResponse> response = given()
            .request()
            .pathParam("groupId", group.getId())
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/category-groups/{groupId}/statistics")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(CATEGORY_STATISTICS_RESPONSE_LIST);

        // then: the request is passed to the service
        verify(categoryService).getStatistics(userId, group.getId(), startDate, endDate);

        // and: the result is as expected
        assertEquals(expectedResult.size(), response.size());
        expectedResult.forEach(expected -> {
            CategoryStatisticsResponse actual = response.stream()
                .filter(s -> expected.getCategoryId().equals(s.getCategoryId()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getGroupId(), actual.getGroupId());
            assertEquals(expected.getGroupName(), actual.getGroupName());
            assertEquals(expected.getCategory(), actual.getCategoryName());
            assertEquals(expected.getCount(), actual.getCount());
            assertEquals(expected.getTotal().doubleValue(), actual.getTotal());
            assertEquals(expected.getCredit().doubleValue(), actual.getCredit());
            assertEquals(expected.getDebit().doubleValue(), actual.getDebit());
        });
    }

    private List<CategoryGroup> mockCategoryGroups(UUID userId, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategoryGroup(userId, insecure().nextAlphanumeric(20)))
            .toList();
    }

    private List<Category> mockCategories(CategoryGroup group, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategory(group, insecure().nextAlphanumeric(20)))
            .toList();
    }

    private CategoryGroup mockCategoryGroup(UUID userId, String name) {
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name(name)
            .description(insecure().nextAlphanumeric(20))
            .build();
        when(categoryService.getCategoryGroup(userId, group.getId()))
            .thenReturn(group);
        return group;
    }

    private Category mockCategory(CategoryGroup group, String name) {
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .group(group)
            .name(name)
            .description(insecure().nextAlphanumeric(20))
            .colour(insecure().nextAlphanumeric(20))
            .build();
        when(categoryService.getCategory(group.getUserId(), category.getId()))
            .thenReturn(category);
        return category;
    }

    private List<CategorySelector> mockCategorySelectors(int size, UUID accountId, UUID categoryId) {
        Category category = Category.builder()
            .id(categoryId)
            .name(insecure().nextAlphanumeric(20))
            .build();
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategorySelector(accountId, category))
            .toList();
    }

    private CategorySelector mockCategorySelector(UUID accountId, Category category) {
        return CategorySelector.builder()
            .id(UUID.randomUUID())
            .accountId(accountId)
            .category(category)
            .infoContains(insecure().nextAlphanumeric(20))
            .build();
    }
}
