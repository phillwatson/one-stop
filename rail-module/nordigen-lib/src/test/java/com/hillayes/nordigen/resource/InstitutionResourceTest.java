package com.hillayes.nordigen.resource;

import com.hillayes.onestop.api.PageLinks;
import com.hillayes.onestop.api.PaginatedInstitutions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class InstitutionResourceTest extends TestResourceBase {
    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testListBanks() {
        PaginatedInstitutions response = given()
            .queryParam("country", "GB")
            .queryParam("page", 0)
            .queryParam("page-size", 10)
            .when().get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedInstitutions.class);

        // and: the response corresponds to the paged list of accounts
        assertEquals(107, response.getTotal());
        assertEquals(10, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(10, response.getItems().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getPageSize());

        // and: all page links are present - there is only one page
        PageLinks links = response.getLinks();
        assertEquals("/api/v1/rails/institutions", links.getFirst().getPath());
        assertTrue(links.getFirst().getQuery().contains("country=GB"));
        assertTrue(links.getFirst().getQuery().contains("page-size=10"));
        assertTrue(links.getFirst().getQuery().contains("page=0"));

        assertNull(links.getPrevious());

        assertNotNull(links.getNext());
        assertEquals("/api/v1/rails/institutions", links.getNext().getPath());
        assertTrue(links.getNext().getQuery().contains("country=GB"));
        assertTrue(links.getNext().getQuery().contains("page-size=10"));
        assertTrue(links.getNext().getQuery().contains("page=1"));

        assertEquals("/api/v1/rails/institutions", links.getLast().getPath());
        assertTrue(links.getLast().getQuery().contains("country=GB"));
        assertTrue(links.getLast().getQuery().contains("page-size=10"));
        assertTrue(links.getLast().getQuery().contains("page=10"));
    }

    @Test
    @TestSecurity(user = TestResourceBase.adminIdStr, roles = "admin")
    public void testGetBank() {
        given()
            .pathParam("id", "FIRST_DIRECT_MIDLGB22")
            .when().get("/api/v1/rails/institutions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("name", is("First Direct"));
    }
}
