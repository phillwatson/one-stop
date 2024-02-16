package com.hillayes.rail.resource;

import com.hillayes.commons.Strings;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.utils.TestApiData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class InstitutionResourceTest extends TestBase {
    @InjectMock
    InstitutionService institutionService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testAll_AsUser() {
        // given: a list of institutions
        List<RailInstitution> institutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(false)))
            .toList();
        when(institutionService.list(any(), any(), eq(false)))
            .thenReturn(institutions);

        // when: client calls the endpoint
        PaginatedInstitutions response = given()
            .request()
            .queryParam("page", 2)
            .queryParam("page-size", 5)
            .queryParam("country", "GB")
            .queryParam("rail", "NORDIGEN")
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedInstitutions.class);

        // then: the institution service is called for country code - one for payments enabled and one for disabled
        verify(institutionService, times(2)).list(eq(RailProvider.NORDIGEN), eq("GB"), anyBoolean());

        // and: the response corresponds to the paged list of accounts
        assertEquals(5, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(5, response.getItems().size());
        assertEquals(institutions.size(), response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getPageSize());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "admin")
    public void testAll_AsAdmin() {
        // given: a list of institutions
        List<RailInstitution> institutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(false)))
            .toList();
        when(institutionService.list(any(), any(), eq(false)))
            .thenReturn(institutions);

        // when: client calls the endpoint
        PaginatedInstitutions response = given()
            .request()
            .queryParam("page", 2)
            .queryParam("page-size", 5)
            .queryParam("country", "GB")
            .queryParam("rail", "NORDIGEN")
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedInstitutions.class);

        // then: the institution service is called for country code - one for payments enabled and one for disabled
        verify(institutionService, times(2)).list(eq(RailProvider.NORDIGEN), eq("GB"), anyBoolean());

        // and: the response corresponds to the paged list of accounts
        assertEquals(5, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(5, response.getItems().size());
        assertEquals(institutions.size(), response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getPageSize());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GB", "PT", ""})
    @TestSecurity(user = userIdStr, roles = "user")
    public void testAll_DefaultCountry_NoSpecificRail(String countryCode) {
        // country code defaults to GB
        String expectedCountryCode = Strings.isBlank(countryCode) ? "GB" : countryCode;

        // given: a list of institutions that don't allow payments
        List<RailInstitution> paymentDisabledInstitutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(false)))
            .toList();
        when(institutionService.list(isNull(), any(), eq(false)))
            .thenReturn(paymentDisabledInstitutions);

        // and: a list of institutions that do allow payments
        List<RailInstitution> paymentEnabledInstitutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(true)))
            .toList();
        when(institutionService.list(isNull(), any(), eq(true)))
            .thenReturn(paymentEnabledInstitutions);

        // when: client calls the endpoint
        RequestSpecification request = given()
            .request()
            .queryParam("page", 2)
            .queryParam("page-size", 5);
        if (Strings.isNotBlank(countryCode)) {
            request.queryParam("country", countryCode);
        }
        PaginatedInstitutions response = request
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedInstitutions.class);

        // then: the institution service is called to get non-payment enabled institutions for country code
        verify(institutionService).list(isNull(), eq(expectedCountryCode), eq(false));

        // and: the institution service is called to get payment enabled institutions for country code
        verify(institutionService).list(isNull(), eq(expectedCountryCode), eq(true));

        // and: no other calls to the institution service are made
        verifyNoMoreInteractions(institutionService);

        // and: the response corresponds to the paged list of accounts
        assertEquals(5, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(5, response.getItems().size());
        assertEquals(paymentEnabledInstitutions.size() + paymentDisabledInstitutions.size(), response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getPageSize());

        // and: all page links are present
        PageLinks links = response.getLinks();
        assertEquals("/api/v1/rails/institutions", links.getFirst().getPath());
        assertTrue(links.getFirst().getQuery().contains("page-size=5"));
        assertTrue(links.getFirst().getQuery().contains("page=0"));
        if (Strings.isNotBlank(countryCode)) {
            assertTrue(links.getFirst().getQuery().contains("country=" + countryCode));
        }

        assertNotNull(links.getPrevious());
        assertEquals("/api/v1/rails/institutions", links.getPrevious().getPath());
        assertTrue(links.getPrevious().getQuery().contains("page-size=5"));
        assertTrue(links.getPrevious().getQuery().contains("page=1"));
        if (Strings.isNotBlank(countryCode)) {
            assertTrue(links.getPrevious().getQuery().contains("country=" + countryCode));
        }

        assertNotNull(links.getNext());
        assertEquals("/api/v1/rails/institutions", links.getNext().getPath());
        assertTrue(links.getNext().getQuery().contains("page-size=5"));
        assertTrue(links.getNext().getQuery().contains("page=3"));
        if (Strings.isNotBlank(countryCode)) {
            assertTrue(links.getNext().getQuery().contains("country=" + countryCode));
        }

        assertEquals("/api/v1/rails/institutions", links.getLast().getPath());
        assertTrue(links.getLast().getQuery().contains("page-size=5"));
        assertTrue(links.getLast().getQuery().contains("page=8"));
        if (Strings.isNotBlank(countryCode)) {
            assertTrue(links.getLast().getQuery().contains("country=" + countryCode));
        }
    }

    @ParameterizedTest
    @EnumSource(RailProvider.class)
    @TestSecurity(user = userIdStr, roles = "user")
    public void testAll_SpecificRail(RailProvider railProvider) {
        // given: a list of institutions that don't allow payments
        List<RailInstitution> paymentDisabledInstitutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(false)))
            .toList();
        when(institutionService.list(eq(railProvider), any(), eq(false)))
            .thenReturn(paymentDisabledInstitutions);

        // and: a list of institutions that do allow payments
        List<RailInstitution> paymentEnabledInstitutions = Stream
            .iterate(1, (n) -> n + 1)
            .limit(21)
            .map(n -> TestApiData.mockInstitution(i -> i.paymentsEnabled(true)))
            .toList();
        when(institutionService.list(eq(railProvider), any(), eq(true)))
            .thenReturn(paymentEnabledInstitutions);

        // when: client calls the endpoint
        PaginatedInstitutions response = given()
            .request()
            .queryParam("page", 2)
            .queryParam("page-size", 5)
            .queryParam("rail", railProvider)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/institutions")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedInstitutions.class);

        // then: the institution service is called to get non-payment enabled institutions for rail provider
        verify(institutionService).list(eq(railProvider), eq("GB"), eq(false));

        // and: the institution service is called to get payment enabled institutions for rail provider
        verify(institutionService).list(eq(railProvider), eq("GB"), eq(true));

        // and: no other calls to the institution service are made
        verifyNoMoreInteractions(institutionService);

        // and: the response corresponds to the paged list of accounts
        assertEquals(5, response.getCount());
        assertNotNull(response.getItems());
        assertEquals(5, response.getItems().size());
        assertEquals(paymentEnabledInstitutions.size() + paymentDisabledInstitutions.size(), response.getTotal());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getPageSize());

        // and: all page links are present
        PageLinks links = response.getLinks();
        assertEquals("/api/v1/rails/institutions", links.getFirst().getPath());
        assertTrue(links.getFirst().getQuery().contains("page-size=5"));
        assertTrue(links.getFirst().getQuery().contains("page=0"));
        assertTrue(links.getFirst().getQuery().contains("rail=" + railProvider));
        assertFalse(links.getFirst().getQuery().contains("country="));

        assertNotNull(links.getPrevious());
        assertEquals("/api/v1/rails/institutions", links.getPrevious().getPath());
        assertTrue(links.getPrevious().getQuery().contains("page-size=5"));
        assertTrue(links.getPrevious().getQuery().contains("page=1"));
        assertTrue(links.getPrevious().getQuery().contains("rail=" + railProvider));
        assertFalse(links.getPrevious().getQuery().contains("country="));

        assertNotNull(links.getNext());
        assertEquals("/api/v1/rails/institutions", links.getNext().getPath());
        assertTrue(links.getNext().getQuery().contains("page-size=5"));
        assertTrue(links.getNext().getQuery().contains("page=3"));
        assertTrue(links.getNext().getQuery().contains("rail=" + railProvider));
        assertFalse(links.getNext().getQuery().contains("country="));

        assertEquals("/api/v1/rails/institutions", links.getLast().getPath());
        assertTrue(links.getLast().getQuery().contains("page-size=5"));
        assertTrue(links.getLast().getQuery().contains("page=8"));
        assertTrue(links.getLast().getQuery().contains("rail=" + railProvider));
        assertFalse(links.getLast().getQuery().contains("country="));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetById() {
        // given: an institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(institution.getId()))
            .thenReturn(java.util.Optional.of(institution));

        // when: client calls the endpoint
        InstitutionResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", institution.getId())
            .get("/api/v1/rails/institutions/{id}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(InstitutionResponse.class);

        // then: the institution service is called to get the institution by id
        verify(institutionService).get(institution.getId());

        // and: no other calls to the institution service are made
        verifyNoMoreInteractions(institutionService);

        // and: the response corresponds to the institution
        assertEquals(institution.getId(), response.getId());
        assertEquals(institution.getProvider().name(), response.getProvider());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetById_NotFound() {
        // given: an institution
        String institutionId = UUID.randomUUID().toString();
        when(institutionService.get(institutionId))
            .thenReturn(java.util.Optional.empty());

        // when: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("id", institutionId)
            .get("/api/v1/rails/institutions/{id}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: the institution service is called to get the institution by id
        verify(institutionService).get(institutionId);

        // and: no other calls to the institution service are made
        verifyNoMoreInteractions(institutionService);

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response identifies the institution entity
        assertNotFoundError(response, contextAttributes -> {
            assertEquals("Institution", contextAttributes.get("entity-type"));
            assertEquals(institutionId, contextAttributes.get("entity-id"));
        });
    }
}
