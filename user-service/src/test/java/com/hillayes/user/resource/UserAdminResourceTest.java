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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserAdminResourceTest extends TestBase {
    @InjectMock
    private UserService userService;

    @Test
    @TestSecurity(user = adminIdStr, roles = "admin")
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
        assertEquals("/api/v1/users", response.getLinks().getFirst().getPath());
        assertEquals("page-size=15&page=0", response.getLinks().getFirst().getQuery());

        assertNotNull(response.getLinks().getPrevious());
        assertEquals("/api/v1/users", response.getLinks().getPrevious().getPath());
        assertEquals("page-size=15&page=9", response.getLinks().getPrevious().getQuery());

        assertNotNull(response.getLinks().getNext());
        assertEquals("/api/v1/users", response.getLinks().getNext().getPath());
        assertEquals("page-size=15&page=11", response.getLinks().getNext().getQuery());

        assertEquals("/api/v1/users", response.getLinks().getLast().getPath());
        assertEquals("page-size=15&page=20", response.getLinks().getLast().getQuery());
    }
}
