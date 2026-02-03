package com.hillayes.shares.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.PaginatedPortfolios;
import com.hillayes.onestop.api.PortfolioRequest;
import com.hillayes.onestop.api.PortfolioResponse;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.service.PortfolioService;
import com.hillayes.shares.service.ShareIndexService;
import com.hillayes.shares.service.ShareTradeService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.randomStrings;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PortfolioResourceTest extends TestBase {
    @InjectMock
    PortfolioService portfolioService;

    @InjectMock
    ShareIndexService shareIndexService;

    @InjectMock
    ShareTradeService shareTradeService;

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolios() {
        // Given: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a page range
        int page = 1;
        int pageSize = 12;

        // And: a list of portfolios
        List<Portfolio> portfolios = IntStream.range(1, 30)
            .mapToObj(i -> mockPortfolio(userId, p -> p.id(UUID.randomUUID())))
            .toList();
        when(portfolioService.listPortfolios(any(), anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(1);
            int size = invocation.getArgument(2);
            return Page.of(portfolios, pageIndex, size);
        });

        // When: the resource is called to get the portfolios
        PaginatedPortfolios response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .when()
            .get("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedPortfolios.class);

        // Then: the portfolio service is called with the authenticated user-id and page
        verify(portfolioService).listPortfolios(userId, page, pageSize);

        // And: the page is returned
        assertNotNull(response);
        assertEquals(page, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(3, response.getTotalPages());
        assertEquals(portfolios.size(), response.getTotal());
        assertEquals(pageSize, response.getCount());

        assertNotNull(response.getItems());
        assertEquals(pageSize, response.getItems().size());

        assertNotNull(response.getLinks().getFirst());
        assertNotNull(response.getLinks().getPrevious());
        assertNotNull(response.getLinks().getNext());
        assertNotNull(response.getLinks().getLast());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreatePortfolio() {
        // Given: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a new portfolio request
        PortfolioRequest request = new PortfolioRequest()
            .name(randomStrings.next(20));

        // And: the service is primed
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()).name(request.getName()));
        when(portfolioService.createPortfolio(userId, request.getName()))
            .thenReturn(portfolio);

        // When: the resource is called to create the portfolio
        PortfolioResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the service is called to create the portfolio
        verify(portfolioService).createPortfolio(userId, request.getName());

        // And: the new portfolio is returned
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertNotNull(response.getDateCreated());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdatePortfolio() {
        // Given: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a new portfolio request
        PortfolioRequest request = new PortfolioRequest()
            .name(randomStrings.next(20));

        // And: the service is primed
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()).name(request.getName()));
        when(portfolioService.updatePortfolio(userId, portfolio.getId(), request.getName()))
            .thenReturn(Optional.of(portfolio));

        // When: the resource is called to update the portfolio
        PortfolioResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .put("/api/v1/shares/portfolios/{portfolioId}", portfolio.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the service is called to update the portfolio
        verify(portfolioService).updatePortfolio(userId, portfolio.getId(), request.getName());

        // And: the updated portfolio is returned
        assertNotNull(response);
        assertEquals(portfolio.getId(), response.getId());
        assertEquals(portfolio.getName(), response.getName());
        assertEquals(portfolio.getDateCreated(), response.getDateCreated());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolio() {
        // Given: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: the service is primed
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.getPortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // When: the resource is called to update the portfolio
        PortfolioResponse response = given()
            .request()
            .when()
            .get("/api/v1/shares/portfolios/{portfolioId}", portfolio.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the service is called to update the portfolio
        verify(portfolioService).getPortfolio(userId, portfolio.getId());

        // And: the portfolio is returned
        assertNotNull(response);
        assertEquals(portfolio.getId(), response.getId());
        assertEquals(portfolio.getName(), response.getName());
        assertEquals(portfolio.getDateCreated(), response.getDateCreated());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeletePortfolio() {
        // Given: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: the service is primed
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.deletePortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // When: the resource is called to update the portfolio
        given()
            .request()
            .when()
            .delete("/api/v1/shares/portfolios/{portfolioId}", portfolio.getId())
            .then()
            .statusCode(204);

        // Then: the service is called to update the portfolio
        verify(portfolioService).deletePortfolio(userId, portfolio.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolioHoldings() {
        fail("Not yet implemented");
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetShareTrades() {
        fail("Not yet implemented");
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreateShareTrade() {
        fail("Not yet implemented");
    }
}
