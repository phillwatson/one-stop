package com.hillayes.rail.resource;

import com.hillayes.onestop.api.CountryResponse;
import com.hillayes.onestop.api.PageLinks;
import com.hillayes.onestop.api.PaginatedCountries;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CountryResourceTest extends TestBase {
    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testListCountries() {
        PaginatedCountries response = given()
            .when()
            .queryParam("page", 0)
            .queryParam("page-size", 20)
            .get("/api/v1/rails/countries")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedCountries.class);

        // and: the response corresponds to the paged list of accounts
        assertEquals(4, response.getTotal());
        assertEquals(4, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(4, response.getItems().size());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getPageSize());

        // and: all page links are present - there is only one page
        PageLinks links = response.getLinks();
        assertEquals("/api/v1/rails/countries", links.getFirst().getPath());
        assertTrue(links.getFirst().getQuery().contains("page-size=20"));
        assertTrue(links.getFirst().getQuery().contains("&page=0"));

        assertNull(links.getPrevious());
        assertNull(links.getNext());

        assertEquals("/api/v1/rails/countries", links.getLast().getPath());
        assertTrue(links.getLast().getQuery().contains("page-size=20"));
        assertTrue(links.getLast().getQuery().contains("&page=0"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCountry() {
        CountryResponse response = given()
            .pathParam("id", "GB")
            .when().get("/api/v1/rails/countries/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(CountryResponse.class);

        assertEquals("GB", response.getId());
        assertEquals("Great Britain", response.getName());
        assertNotNull(response.getFlagUri());
        assertEquals("/api/v1/rails/countries/GB/logos", response.getFlagUri().getPath());
        assertEquals("version=1", response.getFlagUri().getQuery());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetCountryLogo() {
        Response response = given()
            .pathParam("id", "GB")
            .accept("image/png")
            .when().get("/api/v1/rails/countries/{id}/logos")
            .then()
            .statusCode(200)
            .contentType("image/png")
            .header("Content-Disposition", "attachment; filename=\"GB.png\"")
            .header("Cache-Control", "no-transform, s-maxage=31536000, max-age=31536000")
            .extract().response();

        assertEquals(200, response.statusCode());

        byte[] fileContents = response.getBody().asByteArray();
        assertNotNull(fileContents);
        assertTrue(fileContents.length > 0);
    }
}
