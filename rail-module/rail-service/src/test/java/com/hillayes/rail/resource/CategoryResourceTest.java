package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.domain.Category;
import com.hillayes.rail.domain.CategorySelector;
import com.hillayes.rail.service.CategoryService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    public void testGetCategories() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

        // and: a page range
        int page = 2;
        int pageSize = 19;

        // and: a paginated list of categories
        Page<Category> categories = Page.of(mockCategories(40), page, pageSize);
        when(categoryService.getCategories(userId, page, pageSize))
            .thenReturn(categories);

        // when: a paginated list of categories is requested
        PaginatedCategories response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/categories")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCategories.class);

        // then: the category-service is called with the authenticated user-id and page
        verify(categoryService).getCategories(userId, page, pageSize);

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

        // and: an existing category
        Category category = mockCategory(randomAlphanumeric(20));
        when(categoryService.getCategory(userId, category.getId()))
            .thenReturn(category);

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
        assertEquals(category.getName(), response.getName());
        assertEquals(category.getDescription(), response.getDescription());
        assertEquals(category.getColour(), response.getColour());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreateCategory() {
        // given: an authenticated user
        UUID userId = UUID.fromString(userIdStr);

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
        when(categoryService.createCategory(userId, request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: a new category is created
        String location = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/rails/categories")
            .then()
            .statusCode(201)
            .extract().header("Location");

        // then: the category-service is called with the authenticated user-id and new category details
        verify(categoryService).createCategory(userId, request.getName(), request.getDescription(), request.getColour());

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

        // and: the service is mocked to return the new category
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription())
            .colour(request.getColour()).build();
        when(categoryService.createCategory(userId, request.getName(), request.getDescription(), request.getColour()))
            .thenReturn(category);

        // when: a new category is created
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

        // when: the category selectors are updated
        given()
            .request()
            .pathParam("categoryId", categoryId)
            .pathParam("accountId", accountId)
            .contentType(JSON)
            .body(selectors)
            .when()
            .put("/api/v1/rails/categories/{categoryId}/selectors/{accountId}")
            .then()
            .statusCode(204);

        // then: the category-service is called with the authenticated user-id, category-id, account-id, and new category-selectors
        ArgumentCaptor<List<CategorySelector>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryService).setCategorySelectors(eq(userId), eq(categoryId), eq(accountId), captor.capture());

        // and: the new category-selectors are passed to the category-service
        List<CategorySelector> actual = captor.getValue();
        assertEquals(selectors.size(), actual.size());
    }

    private List<Category> mockCategories(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> mockCategory(randomAlphanumeric(20)))
            .toList();
    }

    private Category mockCategory(String name) {
        return Category.builder()
            .id(UUID.randomUUID())
            .name(name)
            .description(randomAlphanumeric(20))
            .colour(randomAlphanumeric(20))
            .build();
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
