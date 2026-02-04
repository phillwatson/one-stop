package com.hillayes.shares.resource;

import com.hillayes.commons.jpa.Page;
import com.hillayes.onestop.api.*;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareTrade;
import com.hillayes.shares.domain.ShareTradeSummary;
import com.hillayes.shares.service.PortfolioService;
import com.hillayes.shares.service.ShareIndexService;
import com.hillayes.shares.service.ShareTradeService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PortfolioResourceTest extends TestBase {
    private static final TypeRef<List<ShareTradeSummaryResponse>> SHARE_TRADE_SUMMARY_LIST = new TypeRef<>() {
    };

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

        // And: a portfolio exists
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

        // And: a portfolio exists
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

        // And: a portfolio exists
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.getPortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // When: the resource is called to get the portfolio
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

        // And: a portfolio exists
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.deletePortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // When: the resource is called to delete the portfolio
        given()
            .request()
            .when()
            .delete("/api/v1/shares/portfolios/{portfolioId}", portfolio.getId())
            .then()
            .statusCode(204);

        // Then: the service is called to delete the portfolio
        verify(portfolioService).deletePortfolio(userId, portfolio.getId());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolioHoldings() {
        // Given: a collection shares
        List<ShareIndex> indices = IntStream.range(0, 5)
            .mapToObj(i -> mockShareIndex(s -> s.id(UUID.randomUUID())))
            .toList();

        // And: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a portfolio exists
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.getPortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: share trades exist within the portfolio
        List<ShareTradeSummary> summaries = indices.stream()
            .map(index -> mockShareTradeSummary(portfolio, index))
            .toList();
        when(shareTradeService.getShareTradeSummaries(portfolio))
            .thenReturn(summaries);

        // When: the resource is called to get the trade summaries
        List<ShareTradeSummaryResponse> response = given()
            .request()
            .when()
            .get("/api/v1/shares/portfolios/{portfolioId}/trades", portfolio.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(SHARE_TRADE_SUMMARY_LIST);

        // Then: the services are called
        verify(portfolioService).getPortfolio(userId, portfolio.getId());
        verify(shareTradeService).getShareTradeSummaries(portfolio);

        // And: the summaries are returned
        assertNotNull(response);
        assertEquals(response.size(), summaries.size());
        response.forEach(summary -> {
            ShareTradeSummary expected = summaries.stream()
                .filter(s -> s.getShareIndexId().equals(summary.getShareIndexId()))
                .findAny().orElse(null);
            assertNotNull(expected);

            assertEquals(expected.getPortfolioId(), summary.getPortfolioId());
            assertEquals(expected.getShareIndexId(), summary.getShareIndexId());
            assertEquals(expected.getShareIdentity().getIsin(), summary.getShareId().getIsin());
            assertEquals(expected.getShareIdentity().getTickerSymbol(), summary.getShareId().getTickerSymbol());
            assertEquals(expected.getName(), summary.getName());
            assertEquals(expected.getQuantity(), summary.getQuantity());
            assertEquals(expected.getTotalCost().doubleValue(), summary.getTotalCost());
            assertEquals(expected.getCurrency().getCurrencyCode(), summary.getCurrency());
            assertEquals(expected.getLatestPrice().doubleValue(), summary.getLatestPrice());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetShareTrades() {
        // Given: a share index
        ShareIndex index = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(index.getId()))
            .thenReturn(Optional.of(index));

        // And: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a portfolio exists
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.getPortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        // And: a page range
        int page = 1;
        int pageSize = 10;

        // And: trades exist within the portfolio for the share
        List<ShareTrade> shareTrades = IntStream.range(0, 25)
            .mapToObj(i -> mockShareTrade(portfolio, index, t -> t.id(UUID.randomUUID())))
            .toList();
        when(shareTradeService.getShareTrades(eq(portfolio), eq(index), anyInt(), anyInt())).then(invocation -> {
            int pageIndex = invocation.getArgument(2);
            int size = invocation.getArgument(3);
            return Page.of(shareTrades, pageIndex, size);
        });

        // When: the trades for the share are requested
        PaginatedShareTrades response = given()
            .request()
            .queryParam("page", page)
            .queryParam("page-size", pageSize)
            .when()
            .get("/api/v1/shares/portfolios/{portfolioId}/trades/{shareIndexId}", portfolio.getId(), index.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedShareTrades.class);

        // Then: the services are called
        verify(shareIndexService).getShareIndex(index.getId());
        verify(portfolioService).getPortfolio(userId, portfolio.getId());
        verify(shareTradeService).getShareTrades(portfolio, index, page, pageSize);

        // And: the page is returned
        assertNotNull(response);
        assertEquals(page, response.getPage());
        assertEquals(pageSize, response.getPageSize());
        assertEquals(3, response.getTotalPages());
        assertEquals(shareTrades.size(), response.getTotal());
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
    public void testCreateShareTrade() {
        // Given: a share index
        ShareIndex index = mockShareIndex(s -> s.id(UUID.randomUUID()));
        when(shareIndexService.getShareIndex(index.getId()))
            .thenReturn(Optional.of(index));

        // And: a user ID
        UUID userId = UUID.fromString(userIdStr);

        // And: a portfolio exists
        Portfolio portfolio = mockPortfolio(userId,
            p -> p.id(UUID.randomUUID()));
        when(portfolioService.getPortfolio(userId, portfolio.getId()))
            .thenReturn(Optional.of(portfolio));

        when(shareTradeService.recordShareTrade(eq(portfolio), eq(index),
            any(LocalDate.class), anyInt(), any(BigDecimal.class)))
            .then(invocation ->
                ShareTrade.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .portfolioId(portfolio.getId())
                    .shareIndexId(index.getId())
                    .quantity(invocation.getArgument(3))
                    .price(invocation.getArgument(4))
                    .dateExecuted(invocation.getArgument(2))
                    .build()
            );

        // And: a request to create a new trade
        ShareTradeRequest request = new ShareTradeRequest()
            .shareIndexId(index.getId())
            .dateExecuted(LocalDate.now().minusDays(10))
            .quantity(randomNumbers.randomInt(2, 200))
            .pricePerShare(randomNumbers.randomDouble(1000, 2000));

        // When: the trades for the share are requested
        ShareTradeResponse response = given()
            .request()
            .contentType(JSON)
            .body(request)
            .when()
            .post("/api/v1/shares/portfolios/{portfolioId}/trades", portfolio.getId())
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(ShareTradeResponse.class);

        // Then: the services are called
        verify(shareIndexService).getShareIndex(index.getId());
        verify(portfolioService).getPortfolio(userId, portfolio.getId());
        verify(shareTradeService).recordShareTrade(portfolio, index,
            request.getDateExecuted(), request.getQuantity(), BigDecimal.valueOf(request.getPricePerShare()));

        // And: the new trade is returned
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getShareIndexId(), response.getShareIndexId());
        assertEquals(request.getDateExecuted(), response.getDateExecuted());
        assertEquals(request.getQuantity(), response.getQuantity());
        assertEquals(request.getPricePerShare(), response.getPricePerShare());
    }
}
