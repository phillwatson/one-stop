package com.hillayes.user.resource;

import com.hillayes.auth.xsrf.XsrfValidator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
public class UserAdminResourceTest {
    @InjectMock
    XsrfValidator xsrfValidator;

    @BeforeEach
    public void init() {
        Mockito.when(xsrfValidator.validateToken(Mockito.any(ContainerRequestContext.class))).thenReturn(true);
    }

    @AfterEach
    public void cleanup() {
    }

    @Test
    public void testListUsers() {
        String response = given()
            .when().get("/api/v1/users")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().asString();
        System.out.println(response);
    }
}
