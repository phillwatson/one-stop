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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CategoryResourceTest extends TestBase {
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
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

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
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20));

        // and: the service is mocked to return the new category group
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription()).build();
        when(categoryService.createCategoryGroup(userId, request.getName(), request.getDescription()))
            .thenReturn(group);

        // when: a new category is created
        String location = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/rails/category-groups")
            .then()
            .statusCode(201)
            .extract().header("Location");

        // then: the category-service is called with the authenticated user-id and new category group details
        verify(categoryService).createCategoryGroup(userId, request.getName(), request.getDescription());

        // and: the new category group locator is returned
        assertTrue(location.contains("/api/v1/rails/category-groups/" + group.getId().toString()));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group update request
        CategoryGroupRequest request = new CategoryGroupRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20));

        // and: a category group to be updated
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription()).build();
        when(categoryService.updateCategoryGroup(userId, group.getId(), request.getName(), request.getDescription()))
            .thenReturn(group);

        // when: the category is updated
        given()
            .request()
            .pathParam("groupId", group.getId())
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/category-groups/{groupId}")
            .then()
            .statusCode(204);

        // then: the category-service is called with the authenticated user-id and updated category group details
        verify(categoryService).updateCategoryGroup(userId, group.getId(),
            request.getName(), request.getDescription());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteCategoryGroup() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group belonging to the user
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20)).build();
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
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

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
        CategoryGroup group = mockCategoryGroup(userId, randomAlphanumeric(20));

        // and: an existing category
        Category category = mockCategory(group, randomAlphanumeric(20));

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

        // and: a category group ID
        UUID groupId = UUID.randomUUID();

        // and: a new category request
        CategoryRequest request = new CategoryRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20))
            .colour(randomAlphanumeric(20));

        // and: the service is mocked to return the new category
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription())
            .colour(request.getColour()).build();
        when(categoryService.createCategory(userId, groupId, request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: a new category is created
        String location = given()
            .request()
            .contentType(JSON)
            .body(request)
            .pathParam("groupId", groupId)
            .when()
            .post("/api/v1/rails/category-groups/{groupId}/categories")
            .then()
            .statusCode(201)
            .extract().header("Location");

        // then: the category-service is called with the authenticated user-id, group-id and new category details
        verify(categoryService).createCategory(userId, groupId, request.getName(), request.getDescription(), request.getColour());

        // and: the new category locator is returned
        assertTrue(location.contains("/api/v1/rails/categories/" + category.getId().toString()));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdateCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category update request
        CategoryRequest request = new CategoryRequest()
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20))
            .colour(randomAlphanumeric(20));

        // and: a category to be updated
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription())
            .colour(request.getColour()).build();
        when(categoryService.updateCategory(userId, category.getId(), request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: the category is updated
        given()
            .request()
            .pathParam("categoryId", category.getId())
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/rails/categories/{categoryId}")
            .then()
            .statusCode(204);

        // then: the category-service is called with the authenticated user-id and updated category details
        verify(categoryService).updateCategory(userId, category.getId(),
            request.getName(), request.getDescription(), request.getColour());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category belonging to the user
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name(randomAlphanumeric(20))
            .description(randomAlphanumeric(20)).build();
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
        UUID accountId = UUID.randomUUID();

        // and: a list of category-selectors
        Collection<CategorySelector> categorySelectors = mockCategorySelectors(5, accountId, categoryId);
        when(categoryService.getCategorySelectors(userId, categoryId, accountId))
            .thenReturn(categorySelectors);

        // when: the category selectors are requested
        TypeRef<Collection<AccountCategorySelector>> typeRef = new TypeRef<>() {};
        Collection<AccountCategorySelector> response = given()
            .request()
            .pathParam("categoryId", categoryId)
            .pathParam("accountId", accountId)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/categories/{categoryId}/selectors/{accountId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(typeRef);

        // then: the category-service is called with the authenticated user-id, category-id, and account-id
        verify(categoryService).getCategorySelectors(userId, categoryId, accountId);

        // and: the response contains the list of category-selectors
        assertEquals(categorySelectors.size(), response.size());
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
        List<AccountCategorySelector> selectors = List.of(
            new AccountCategorySelector().infoContains(randomAlphanumeric(20)),
            new AccountCategorySelector().refContains(randomAlphanumeric(20)),
            new AccountCategorySelector().creditorContains(randomAlphanumeric(20))
        );

        // and: the service is primed with data
        List<CategorySelector> updatedSelectors = mockCategorySelectors(selectors.size(), accountId, categoryId);
        when(categoryService.setCategorySelectors(eq(userId), eq(categoryId), eq(accountId), anyList()))
            .thenReturn(updatedSelectors);

        // when: the category selectors are updated
        TypeRef<Collection<AccountCategorySelector>> typeRef = new TypeRef<>() {};
        Collection<AccountCategorySelector> response = given()
            .request()
            .pathParam("categoryId", categoryId)
            .pathParam("accountId", accountId)
            .contentType(JSON)
            .body(selectors)
            .when()
            .put("/api/v1/rails/categories/{categoryId}/selectors/{accountId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(typeRef);

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
    public void testGetStatistics() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a category group ID
        UUID groupId = UUID.randomUUID();

        // and: a date range
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();

        // and: the service is primed with data
        Instant startDate = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = toDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<CategoryStatistics> expectedResult = List.of(
            TestData.mockCategoryStatistics("cat-1", 20, 123.44, 282.93,11.25),
            TestData.mockCategoryStatistics("cat-2", 10, 456.44, 222.73,21.225),
            TestData.mockCategoryStatistics("cat-3", 6, 34.44, 82.73,177.25)
        );
        when(categoryService.getStatistics(userId, groupId, startDate, endDate))
            .thenReturn(expectedResult);

        // when: the statistics are requested
        TypeRef<List<CategoryStatisticsResponse>> typeRef = new TypeRef<>() {};
        Collection<CategoryStatisticsResponse> response = given()
            .request()
            .pathParam("groupId", groupId)
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/category-groups/{groupId}/statistics")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(typeRef);

        // then: the request is passed to the service
        verify(categoryService).getStatistics(userId, groupId, startDate, endDate);

        // and: the result is as expected
        assertEquals(expectedResult.size(), response.size());
        expectedResult.forEach(expected -> {
            CategoryStatisticsResponse actual = response.stream()
                .filter(s -> expected.getCategoryId().equals(s.getCategoryId()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getCategory(), actual.getCategory());
            assertEquals(expected.getCount(), actual.getCount());
            assertEquals(expected.getTotal().doubleValue(), actual.getTotal());
            assertEquals(expected.getCredit().doubleValue(), actual.getCredit());
            assertEquals(expected.getDebit().doubleValue(), actual.getDebit());
        });
    }

    private List<CategoryGroup> mockCategoryGroups(UUID userId, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategoryGroup(userId, randomAlphanumeric(20)))
            .toList();
    }

    private List<Category> mockCategories(CategoryGroup group, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategory(group, randomAlphanumeric(20)))
            .toList();
    }

    private CategoryGroup mockCategoryGroup(UUID userId, String name) {
        CategoryGroup group = CategoryGroup.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name(name)
            .description(randomAlphanumeric(20))
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
            .description(randomAlphanumeric(20))
            .colour(randomAlphanumeric(20))
            .build();
        when(categoryService.getCategory(group.getUserId(), category.getId()))
            .thenReturn(category);
        return category;
    }

    private List<CategorySelector> mockCategorySelectors(int size, UUID accountId, UUID categoryId) {
        Category category = Category.builder()
            .id(categoryId)
            .name(randomAlphanumeric(20))
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
            .infoContains(randomAlphanumeric(20))
            .build();
    }
}
