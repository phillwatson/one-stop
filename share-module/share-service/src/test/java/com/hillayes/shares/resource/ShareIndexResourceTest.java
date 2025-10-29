package com.hillayes.shares.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.errors.DuplicateIsinException;
import com.hillayes.shares.service.ShareIndexService;
import com.hillayes.shares.service.SharePriceService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockPriceHistory;
import static com.hillayes.shares.utils.TestData.mockShareIndex;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ShareIndexResourceTest extends TestBase {
    private static final RandomStringUtils randomStrings = RandomStringUtils.insecure();
    private static final TypeRef<List<ShareIndexResponse>> SHARE_INDEX_LIST = new TypeRef<>() {};

    @InjectMock
    ShareIndexService shareIndexService;

    @InjectMock
    SharePriceService sharePriceService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testRegisterShareIndices() {
        // Given: a valid request with several share index entries
        List<RegisterShareIndexRequest> request = List.of(
            mockRegisterShareIndexRequest(),
            mockRegisterShareIndexRequest(),
            mockRegisterShareIndexRequest()
        );

        // And: and the service will register the indices
        when(shareIndexService.registerShareIndices(anyList())).thenCallRealMethod();
        when(shareIndexService.registerShareIndex(
            anyString(), anyString(), any(Currency.class), any(ShareProvider.class)))
            .then(invocation -> ShareIndex.builder()
                .id(UUID.randomUUID())
                .isin(invocation.getArgument(0))
                .name(invocation.getArgument(1))
                .currency(invocation.getArgument(2))
                .provider(invocation.getArgument(3))
                .build());

        // When: client calls the endpoint
        List<ShareIndexResponse> response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(SHARE_INDEX_LIST);

        // Then: the share index service is called with the entire list
        verify(shareIndexService).registerShareIndices(anyList());

        // And: the share index service is called for each share in the request
        verify(shareIndexService, times(request.size())).registerShareIndex(
            anyString(), anyString(), any(Currency.class), any(ShareProvider.class)
        );

        // And: the response contains each registered share
        assertNotNull(response);
        assertEquals(request.size(), response.size());
        request.forEach(expected -> {
            ShareIndexResponse actual = response.stream().filter(s -> s.getIsin().equals(expected.getIsin()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertNotNull(actual.getId());
            assertEquals(expected.getIsin(), actual.getIsin());
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getCurrency(), actual.getCurrency());
            assertEquals(expected.getProvider(), actual.getProvider());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testRegisterShareIndices_EmptyRequest() {
        // Given: a valid request with no share index entries
        List<RegisterShareIndexRequest> request = List.of();

        // And: and the service will register the indices
        when(shareIndexService.registerShareIndices(anyList())).thenCallRealMethod();
        when(shareIndexService.registerShareIndex(
            anyString(), anyString(), any(Currency.class), any(ShareProvider.class)))
            .then(invocation -> {
                return ShareIndex.builder()
                    .id(UUID.randomUUID())
                    .isin(invocation.getArgument(0))
                    .name(invocation.getArgument(1))
                    .currency(invocation.getArgument(2))
                    .provider(invocation.getArgument(3))
                    .build();
            });

        // When: client calls the endpoint
        List<ShareIndexResponse> response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(SHARE_INDEX_LIST);

        // Then: the share index service is NEVER called
        verifyNoInteractions(shareIndexService);

        // And: the response contains each registered share
        assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testRegisterShareIndices_Duplicate() {
        // Given: a request with a duplicate share index entries
        RegisterShareIndexRequest duplicateShareIndex = mockRegisterShareIndexRequest();
        List<RegisterShareIndexRequest> request = List.of(
            duplicateShareIndex
        );

        // And: and the service will register the indices
        when(shareIndexService.registerShareIndices(anyList())).thenCallRealMethod();
        when(shareIndexService.registerShareIndex(
            anyString(), anyString(), any(Currency.class), any(ShareProvider.class)))
            .thenThrow(new DuplicateIsinException(request.get(0).getIsin(), null));

        // When: client calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/shares/indices")
            .then()
            .statusCode(409)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // Then: the share index service is called to save the index
        verify(shareIndexService).registerShareIndices(anyList());
        verify(shareIndexService).registerShareIndex(anyString(), anyString(), any(Currency.class), any(ShareProvider.class));

        // And: the response describes the ISIN conflict
        assertNotNull(response);
        assertEquals(ErrorSeverity.INFO, response.getSeverity());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());

        ServiceError serviceError = response.getErrors().get(0);
        assertEquals("DUPLICATE_SHARE_ISIN", serviceError.getMessageId());

        Map<String, String> contextAttributes = serviceError.getContextAttributes();
        assertNotNull(contextAttributes);
        assertEquals(1, contextAttributes.size());
        assertEquals(duplicateShareIndex.getIsin(), contextAttributes.get("isin"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAllShareIndices_MoreThanOnePage() {
        // Given: 50 share indices exist
        List<ShareIndex> shareIndices = IntStream.range(0, 50)
            .mapToObj(i -> mockShareIndex(s -> s.id(UUID.randomUUID())))
            .sorted(Comparator.comparing(ShareIndex::getName))
            .toList();
        when(shareIndexService.listShareIndices(anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(0);
            int pageSize = invocation.getArgument(1);
            return Page.of(shareIndices, pageIndex, pageSize);
        });

        // When: the client calls the endpoint for the third page of size 12
        int pageIndex = 2; // zero-based index
        int pageSize = 12;
        PaginatedShareIndices response = given()
            .request()
            .contentType(JSON)
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .when()
            .get("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedShareIndices.class);

        // Then: the response contains the requested page
        assertNotNull(response);
        assertEquals(shareIndices.size(), response.getTotal());
        assertEquals(5, response.getTotalPages());
        assertEquals(pageSize, response.getCount());
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertNotNull(response.getItems());
        assertEquals(pageSize, response.getItems().size());

        // And: each page link is correct
        assertNotNull(response.getLinks().getFirst());
        assertEquals("/api/v1/shares/indices", response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getFirst().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getFirst().getQuery().contains("page=0"));

        assertNotNull(response.getLinks().getPrevious());
        assertEquals("/api/v1/shares/indices", response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getPrevious().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getPrevious().getQuery().contains("page=1"));

        assertNotNull(response.getLinks().getNext());
        assertEquals("/api/v1/shares/indices", response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getNext().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getNext().getQuery().contains("page=3"));

        assertNotNull(response.getLinks().getLast());
        assertEquals("/api/v1/shares/indices", response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getLast().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getLast().getQuery().contains("page=4"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetAllShareIndices_NoIndicesFound() {
        // Given: no registered Share Indices
        when(shareIndexService.listShareIndices(anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(0);
                int pageSize = invocation.getArgument(1);
                return Page.empty(pageIndex, pageSize);
            });

        // When: the client calls the endpoint for the third page of size 12
        int pageIndex = 2; // zero-based index
        int pageSize = 12;
        PaginatedSharePrices response = given()
            .request()
            .contentType(JSON)
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .when()
            .get("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedSharePrices.class);

        // Then: the response contains the requested page
        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getCount());
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertNotNull(response.getItems());
        assertTrue(response.getItems().isEmpty());

        // And: each page link is correct
        String path = "/api/v1/shares/indices";
        URI link = response.getLinks().getFirst();
        assertNotNull(link);
        assertEquals(path, link.getPath());
        assertTrue(link.getQuery().contains("page-size=12"));
        assertTrue(link.getQuery().contains("page=0"));

        assertNull(response.getLinks().getPrevious());
        assertNull(response.getLinks().getNext());

        link = response.getLinks().getLast();
        assertNotNull(link);
        assertEquals(path, link.getPath());
        assertTrue(link.getQuery().contains("page-size=12"));
        assertTrue(link.getQuery().contains("page=0"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetSharePrices_MoreThanOnePage() {
        // Given: a registered Share Index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(eq(shareIndex.getId())))
            .thenReturn(Optional.of(shareIndex));

        // And: a history of prices for that index
        AtomicReference<LocalDate> date = new AtomicReference<>(LocalDate.now().minusDays(100));
        List<PriceHistory> prices = IntStream.range(0, 100).mapToObj(i -> {
            LocalDate marketDate = date.get();
            PriceHistory priceHistory = mockPriceHistory(shareIndex, marketDate, SharePriceResolution.DAILY);
            date.set(marketDate.plusDays(1));
            return priceHistory;
        }).toList();
        when(sharePriceService.getPrices(eq(shareIndex), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt()))
            .then(invocation -> {
                LocalDate fromDate = invocation.getArgument(1);
                LocalDate toDate = invocation.getArgument(2);
                int pageIndex = invocation.getArgument(3);
                int pageSize = invocation.getArgument(4);
                return Page.of(prices.stream()
                    .filter(p -> (!p.getId().getDate().isBefore(fromDate)) && (p.getId().getDate().isBefore(toDate)))
                    .toList(), pageIndex, pageSize);
            });

        // When: the client calls the endpoint for the third page of size 12
        LocalDate fromDate = LocalDate.now().minusDays(90);
        LocalDate toDate = LocalDate.now().minusDays(5);
        int pageIndex = 2; // zero-based index
        int pageSize = 12;
        PaginatedSharePrices response = given()
            .request()
            .contentType(JSON)
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .when()
            .pathParam("shareId", shareIndex.getId())
            .get("/api/v1/shares/indices/{shareId}/prices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedSharePrices.class);

        // Then: the response contains the requested page
        assertNotNull(response);
        assertEquals(ChronoUnit.DAYS.between(fromDate, toDate), response.getTotal());
        assertEquals(8, response.getTotalPages());
        assertEquals(pageSize, response.getCount());
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertNotNull(response.getItems());
        assertEquals(pageSize, response.getItems().size());

        // And: each page link is correct
        String path = "/api/v1/shares/indices/" + shareIndex.getId() + "/prices";
        assertNotNull(response.getLinks().getFirst());
        assertEquals(path, response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getFirst().getQuery().contains(fromDate.toString()));
        assertTrue(response.getLinks().getFirst().getQuery().contains(toDate.toString()));
        assertTrue(response.getLinks().getFirst().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getFirst().getQuery().contains("page=0"));

        assertNotNull(response.getLinks().getPrevious());
        assertEquals(path, response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getPrevious().getQuery().contains(fromDate.toString()));
        assertTrue(response.getLinks().getPrevious().getQuery().contains(toDate.toString()));
        assertTrue(response.getLinks().getPrevious().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getPrevious().getQuery().contains("page=1"));

        assertNotNull(response.getLinks().getNext());
        assertEquals(path, response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getNext().getQuery().contains(fromDate.toString()));
        assertTrue(response.getLinks().getNext().getQuery().contains(toDate.toString()));
        assertTrue(response.getLinks().getNext().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getNext().getQuery().contains("page=3"));

        assertNotNull(response.getLinks().getLast());
        assertEquals(path, response.getLinks().getFirst().getPath());
        assertTrue(response.getLinks().getLast().getQuery().contains(fromDate.toString()));
        assertTrue(response.getLinks().getLast().getQuery().contains(toDate.toString()));
        assertTrue(response.getLinks().getLast().getQuery().contains("page-size=12"));
        assertTrue(response.getLinks().getLast().getQuery().contains("page=7"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetSharePrices_NoPricesFound() {
        // Given: a registered Share Index
        ShareIndex shareIndex = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(eq(shareIndex.getId())))
            .thenReturn(Optional.of(shareIndex));

        // And: no prices are found for any date range
        when(sharePriceService.getPrices(eq(shareIndex), any(LocalDate.class), any(LocalDate.class), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(3);
                int pageSize = invocation.getArgument(4);
                return Page.empty(pageIndex, pageSize);
            });

        // When: the client calls the endpoint for the third page of size 12
        LocalDate fromDate = LocalDate.now().minusDays(90);
        LocalDate toDate = LocalDate.now().minusDays(5);
        int pageIndex = 2; // zero-based index
        int pageSize = 12;
        PaginatedSharePrices response = given()
            .request()
            .contentType(JSON)
            .queryParam("from-date", fromDate.toString())
            .queryParam("to-date", toDate.toString())
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .when()
            .pathParam("shareId", shareIndex.getId())
            .get("/api/v1/shares/indices/{shareId}/prices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedSharePrices.class);

        // Then: the response contains the requested page
        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getCount());
        assertEquals(pageIndex, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertNotNull(response.getItems());
        assertTrue(response.getItems().isEmpty());

        // And: each page link is correct
        String path = "/api/v1/shares/indices/" + shareIndex.getId() + "/prices";
        URI link = response.getLinks().getFirst();
        assertNotNull(link);
        assertEquals(path, link.getPath());
        assertTrue(link.getQuery().contains(fromDate.toString()));
        assertTrue(link.getQuery().contains(toDate.toString()));
        assertTrue(link.getQuery().contains("page-size=12"));
        assertTrue(link.getQuery().contains("page=0"));

        assertNull(response.getLinks().getPrevious());
        assertNull(response.getLinks().getNext());

        link = response.getLinks().getLast();
        assertNotNull(link);
        assertEquals(path, link.getPath());
        assertTrue(link.getQuery().contains(fromDate.toString()));
        assertTrue(link.getQuery().contains(toDate.toString()));
        assertTrue(link.getQuery().contains("page-size=12"));
        assertTrue(link.getQuery().contains("page=0"));
    }

    private RegisterShareIndexRequest mockRegisterShareIndexRequest() {
        return new RegisterShareIndexRequest()
            .isin(randomStrings.nextAlphanumeric(12))
            .name(randomStrings.nextAlphanumeric(30))
            .currency("GBP")
            .provider(ShareProvider.FT_MARKET_DATA.name());
    }
}
