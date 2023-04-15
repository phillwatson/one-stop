package com.hillayes.user.resource;

import com.hillayes.onestop.api.PaginatedUsers;
import com.hillayes.user.domain.User;
import com.hillayes.user.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.hillayes.user.utils.TestData.mockUsers;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserAdminResourceTest extends TestBase {
    @InjectMock
    private UserService userService;

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
    public void testListUsers() {
        List<User> users = mockUsers(15);

        PageRequest pageRequest = PageRequest.of(10, 15);
        Page<User> pagedUsers = new PageImpl<>(users, pageRequest, 310);
        when(userService.listUsers(anyInt(), anyInt())).thenReturn(pagedUsers);

        PaginatedUsers response = given()
            .when()
            .queryParam("page", pageRequest.getPageNumber())
            .queryParam("page-size", pageRequest.getPageSize())
            .get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedUsers.class);

        // then: the user-service is called with the page parameters
        verify(userService).listUsers(pageRequest.getPageNumber(), pageRequest.getPageSize());

        // and: the response corresponds to the paged list of accounts
        assertEquals(users.size(), response.getCount());
        assertNotNull(response.getItems());
        assertEquals(users.size(), response.getItems().size());
        assertEquals(pagedUsers.getTotalElements(), response.getTotal());
        assertEquals(pageRequest.getPageNumber(), response.getPage());
        assertEquals(pageRequest.getPageSize(), response.getPageSize());

        // and: all page links are present
        assertEquals("/api/v1/users?page=0&page-size=15", response.getLinks().getFirst());
        assertEquals("/api/v1/users?page=11&page-size=15", response.getLinks().getNext());
        assertEquals("/api/v1/users?page=9&page-size=15", response.getLinks().getPrevious());
        assertEquals("/api/v1/users?page=20&page-size=15", response.getLinks().getLast());
    }
}
