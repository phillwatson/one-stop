package com.hillayes.rail.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.ConsentResponse;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.domain.Account;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.AccountService;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.service.UserConsentService;
import com.hillayes.rail.utils.TestApiData;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserConsentResourceTest extends TestBase {
    @InjectMock
    UserConsentService userConsentService;
    @InjectMock
    InstitutionService institutionService;
    @InjectMock
    AccountService accountService;
    @InjectMock
    RailProviderFactory railProviderFactory;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetConsents() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a collection of consents for the authenticated user
        List<UserConsent> consents = Stream.iterate(1, (n) -> n + 1)
            .limit(23)
            .map(n -> TestData.mockUserConsent(userId))
            .toList();
        when(userConsentService.listConsents(eq(userId), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(1);
                int pageSize = invocation.getArgument(2);
                return Page.of(consents, pageIndex, pageSize);
            });

        // and: the institutions exist
        when(institutionService.get(any(), any()))
            .thenReturn(Optional.of(TestApiData.mockInstitution()));

        // and: a page range
        int pageIndex = 1;
        int pageSize = 5;

        // when: client calls the endpoint
        PaginatedUserConsents response = given()
            .request()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .contentType(JSON)
            .when()
            .get("/api/v1/rails/consents")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedUserConsents.class);

        // then: the service is called to retrieve the consents
        verify(userConsentService).listConsents(userId, pageIndex, pageSize);

        // and: the response contains the consents
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(pageSize, response.getCount());
        assertEquals(consents.size(), response.getTotal());
        assertEquals(pageSize, response.getItems().size());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetConsentForInstitution() {
        UUID userId = UUID.fromString(userIdStr);

        // given: a consent exist
        UserConsent userConsent = TestData.mockUserConsent(userId);
        when(userConsentService.getUserConsent(userId, userConsent.getInstitutionId()))
            .thenReturn(Optional.of(userConsent));

        // and: accounts associated with the consent
        List<Account> accounts = List.of(
            TestData.mockAccount(userId, userConsent.getId()),
            TestData.mockAccount(userId, userConsent.getId())
        );
        when(accountService.getAccountsByUserConsent(userConsent))
            .thenReturn(accounts);

        // and: the institution exists
        when(institutionService.get(userConsent.getProvider(), userConsent.getInstitutionId()))
            .thenReturn(Optional.of(TestApiData.mockInstitution()));

        // when: client calls the endpoint
        UserConsentResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("institutionId", userConsent.getInstitutionId())
            .get("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(UserConsentResponse.class);

        // then: a response is returned
        assertNotNull(response);

        // and: the service is called to retrieve the consent
        verify(userConsentService).getUserConsent(userId, userConsent.getInstitutionId());

        // and: the response contains the consent
        assertEquals(userConsent.getInstitutionId(), response.getInstitutionId());

        // and: the response contains the associated accounts
        assertNotNull(response.getAccounts());
        assertEquals(accounts.size(), response.getAccounts().size());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetConsentForInstitution_NotFound() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an institution for which consent does not exist
        String institutionId = UUID.randomUUID().toString();
        when(userConsentService.getUserConsent(userId, institutionId))
            .thenReturn(Optional.empty());

        // when: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("institutionId", institutionId)
            .get("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: a response is returned
        assertNotNull(response);

        // and: the service is called to retrieve the consent
        verify(userConsentService).getUserConsent(userId, institutionId);

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response identifies the user-consent entity
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("UserConsent", contextAttributes.get("entity-type"));
            assertTrue(contextAttributes.get("entity-id").contains(institutionId));
            assertTrue(contextAttributes.get("entity-id").contains(userId.toString()));
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteConsent() {
        UUID userId = UUID.fromString(userIdStr);

        UserConsent userConsent = TestData.mockUserConsent(userId);
        when(userConsentService.getUserConsent(userId, userConsent.getInstitutionId()))
            .thenReturn(Optional.of(userConsent));

        // and: the institution exists
        when(institutionService.get(userConsent.getProvider(), userConsent.getInstitutionId()))
            .thenReturn(Optional.of(TestApiData.mockInstitution()));

        // when: client calls the endpoint
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("institutionId", userConsent.getInstitutionId())
            .delete("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(204);

        // then: the service is called to retrieve the consent
        verify(userConsentService).getUserConsent(userId, userConsent.getInstitutionId());

        // and: the service is called to cancel the consent - without purge
        verify(userConsentService).consentCancelled(userConsent.getId(), false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteConsent_WithPurgeParameter(boolean purge) {
        UUID userId = UUID.fromString(userIdStr);

        UserConsent userConsent = TestData.mockUserConsent(userId);
        when(userConsentService.getUserConsent(userId, userConsent.getInstitutionId()))
            .thenReturn(Optional.of(userConsent));

        // and: the institution exists
        when(institutionService.get(userConsent.getProvider(), userConsent.getInstitutionId()))
            .thenReturn(Optional.of(TestApiData.mockInstitution()));

        // when: client calls the endpoint
        given()
            .request()
            .contentType(JSON)
            .when()
            .queryParam("purge", purge)
            .pathParam("institutionId", userConsent.getInstitutionId())
            .delete("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(204);

        // then: the service is called to retrieve the consent
        verify(userConsentService).getUserConsent(userId, userConsent.getInstitutionId());

        // and: the service is called to cancel the consent - with selected purge option
        verify(userConsentService).consentCancelled(userConsent.getId(), purge);
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeleteConsent_NotFound() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an institution for which consent does not exist
        String institutionId = UUID.randomUUID().toString();
        when(userConsentService.getUserConsent(userId, institutionId))
            .thenReturn(Optional.empty());

        // when: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("institutionId", institutionId)
            .delete("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(404)
            .extract()
            .as(ServiceErrorResponse.class);

        // then: a response is returned
        assertNotNull(response);

        // and: the service is called to retrieve the consent
        verify(userConsentService).getUserConsent(userId, institutionId);

        // and: the response contains the expected error message
        assertNotNull(response);

        // and: the response identifies the user-consent entity
        assertNotFoundError(response, (contextAttributes) -> {
            assertEquals("UserConsent", contextAttributes.get("entity-type"));
            assertTrue(contextAttributes.get("entity-id").contains(institutionId));
            assertTrue(contextAttributes.get("entity-id").contains(userId.toString()));
        });

        // and: the service is NOT called to cancel the consent
        verify(userConsentService, never()).consentCancelled(any(), anyBoolean());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testRegister() {
        UUID userId = UUID.fromString(userIdStr);

        // given: an institution id
        String institutionId = UUID.randomUUID().toString();

        // and: a consent request
        URI callbackUri = URI.create("http://localhost:8080/callback");
        UserConsentRequest consentRequest = new UserConsentRequest();
        consentRequest.setCallbackUri(callbackUri);

        // and: the service returns a consent link
        URI expectedConsentLink = URI.create("http://localhost:8080/consent/RAIL");
        when(userConsentService.register(userId, institutionId, callbackUri))
            .thenReturn(expectedConsentLink);

        // when: client calls the endpoint
        URI response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("institutionId", institutionId)
            .body(consentRequest)
            .post("/api/v1/rails/consents/{institutionId}")
            .then()
            .statusCode(200)
            .extract()
            .as(URI.class);

        // then: a response is returned
        assertNotNull(response);

        // and: the service is called
        verify(userConsentService).register(userId, institutionId, callbackUri);

        // and: the response contains the consent link
        assertEquals(expectedConsentLink, response);
    }

    @Test
    public void testConsentResponse_Given() {
        // given: a rail provider
        RailProvider railProvider = RailProvider.NORDIGEN;

        // and: a rail provider api
        RailProviderApi railProviderApi = mock(RailProviderApi.class);
        when(railProviderFactory.get(railProvider))
            .thenReturn(railProviderApi);

        // and: the rail provider returns a consent response
        ConsentResponse consentResponse = ConsentResponse.builder()
            .consentReference(UUID.randomUUID().toString())
            .build();
        when(railProviderApi.parseConsentResponse(any()))
            .thenReturn(consentResponse);

        // and: the consent service returns a redirect uri
        URI redirectUri = URI.create("http://localhost:8080/callback");
        when(userConsentService.consentGiven(railProviderApi, consentResponse))
            .thenReturn(redirectUri);

        // when: client calls the endpoint
        // then: a redirect is returned
        Response response = given()
            .request()
            .contentType(JSON)
            .when()
            .redirects().follow(false)
            .queryParam("ref", consentResponse.getConsentReference())
            .pathParam("railProvider", railProvider)
            .get("/api/v1/rails/consents/response/{railProvider}")
            .then()
            .statusCode(307)
            .extract().response();

        // and: the redirection is to original client callback
        assertNotNull(response);
        assertEquals(redirectUri.toString(), response.getHeader("Location"));

        // and: the rail provider api is called to parse the consent response
        verify(railProviderApi).parseConsentResponse(any());

        // and: the consent service is called to process the accepted consent
        verify(userConsentService).consentGiven(railProviderApi, consentResponse);

        // and: the consent service is NOT called to process the denied consent
        verify(userConsentService, never()).consentDenied(any(), any());
    }

    @Test
    public void testConsentResponse_Denied() {
        // given: a rail provider
        RailProvider railProvider = RailProvider.NORDIGEN;

        // and: a rail provider api
        RailProviderApi railProviderApi = mock(RailProviderApi.class);
        when(railProviderFactory.get(railProvider))
            .thenReturn(railProviderApi);

        // and: the rail provider returns a consent response
        ConsentResponse consentResponse = ConsentResponse.builder()
            .consentReference(UUID.randomUUID().toString())
            .errorCode("access_denied")
            .errorDescription("User denied access")
            .build();
        when(railProviderApi.parseConsentResponse(any()))
            .thenReturn(consentResponse);

        // and: the consent service returns a redirect uri
        URI redirectUri = URI.create("http://localhost:8080/callback");
        when(userConsentService.consentDenied(railProviderApi, consentResponse))
            .thenReturn(redirectUri);

        // when: client calls the endpoint
        // then: a redirect is returned
        Response response = given()
            .request()
            .contentType(JSON)
            .when()
            .redirects().follow(false)
            .queryParam("ref", consentResponse.getConsentReference())
            .pathParam("railProvider", railProvider)
            .get("/api/v1/rails/consents/response/{railProvider}")
            .then()
            .statusCode(307)
            .extract().response();

        // and: the redirection is to original client callback
        assertNotNull(response);
        assertEquals(redirectUri.toString(), response.getHeader("Location"));

        // and: the rail provider api is called to parse the consent response
        verify(railProviderApi).parseConsentResponse(any());

        // and: the consent service is NOT called to process the accepted consent
        verify(userConsentService, never()).consentGiven(any(), any());

        // and: the consent service is called to process the denied consent
        verify(userConsentService).consentDenied(railProviderApi, consentResponse);
    }

    @Test
    public void testResponseUri() {
        URI uri = UriBuilder
            .fromResource(UserConsentResource.class)
            .scheme("http")
            .host("192.2.2.2")
            .port(5555)
            .path(UserConsentResource.class, "consentResponse")
            .buildFromMap(Map.of("railProvider", RailProvider.NORDIGEN));

        assertEquals("http://192.2.2.2:5555/api/v1/rails/consents/response/NORDIGEN", uri.toString());
    }
}
