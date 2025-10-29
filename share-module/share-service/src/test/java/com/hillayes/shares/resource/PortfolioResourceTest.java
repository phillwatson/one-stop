package com.hillayes.shares.resource;

import com.hillayes.onestop.api.*;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.PortfolioRepository;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PortfolioResourceTest extends TestBase {
    @Inject
    ShareIndexRepository shareIndexRepository;

    @Inject
    PriceHistoryRepository priceHistoryRepository;

    @Inject
    PortfolioRepository portfolioRepository;

    @BeforeEach
    @Transactional
    public void beforeEach() {
        portfolioRepository.deleteAll();
        shareIndexRepository.deleteAll();
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testCreatePortfolio() {
        // Given: a create portfolio request
        PortfolioRequest request = new PortfolioRequest()
            .name(randomStrings.nextAlphanumeric(30));

        // When: the service is called
        PortfolioResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .body(request)
            .post("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the portfolio and its holdings are returned
        assertNotNull(response);

        // And: the name is the same as that given in the request
        assertEquals(request.getName(), response.getName());

        // And: the portfolio ID and date are returned
        assertNotNull(response.getId());
        assertNotNull(response.getDateCreated());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdatePortfolio() {
        // Given: the user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            UUID userId = UUID.fromString(userIdStr);
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            return newPortfolio;
        });

        // And: an update portfolio request
        PortfolioRequest request = new PortfolioRequest()
            .name(randomStrings.nextAlphanumeric(30));

        // When: the service is called
        PortfolioResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .body(request)
            .pathParam("portfolioId", portfolio.getId())
            .put("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the portfolio and its holdings are returned
        assertNotNull(response);

        // And: the name is the same as that given in the request
        assertEquals(request.getName(), response.getName());

        // And: the portfolio ID and date are returned
        assertNotNull(response.getId());
        assertNotNull(response.getDateCreated());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testUpdatePortfolio_WrongUser() {
        // Given: another user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            UUID userId = UUID.randomUUID();
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            return newPortfolio;
        });

        // And: an update portfolio request
        PortfolioRequest request = new PortfolioRequest()
            .name(randomStrings.nextAlphanumeric(30));

        // When: the authenticated user calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .body(request)
            .pathParam("portfolioId", portfolio.getId())
            .put("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // Then: the response indicates the error
        assertNotNull(response);

        assertEquals(ErrorSeverity.INFO, response.getSeverity());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());

        ServiceError serviceError = response.getErrors().get(0);
        assertEquals("ENTITY_NOT_FOUND", serviceError.getMessageId());

        assertNotNull(serviceError.getContextAttributes());
        assertEquals("Portfolio", serviceError.getContextAttributes().get("entity-type"));
        assertEquals(portfolio.getId().toString(), serviceError.getContextAttributes().get("entity-id"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolios() {
        // Given: the user has several portfolios
        UUID userId = UUID.fromString(userIdStr);
        List<Portfolio> portfolios = withTransaction(() -> {
            List<Portfolio> list = IntStream.range(0, 5)
                .mapToObj(i -> mockPortfolio(userId))
                .toList();

            portfolioRepository.saveAll(list);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();
            return list;
        });

        // When: the user calls the endpoint
        PaginatedPortfolios response = given()
            .request()
            .contentType(JSON)
            .when()
            .queryParam("page", 0)
            .queryParam("page-size", 100)
            .get("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedPortfolios.class);

        // Then: the user's portfolios are returned
        assertNotNull(response);

        assertEquals(portfolios.size(), response.getCount());
        portfolios.forEach(expected -> {
            PortfolioSummaryResponse actual = response.getItems().stream()
                .filter(p -> p.getId().equals(expected.getId()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
            assertNotNull(actual.getDateCreated());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolios_WrongUser() {
        // Given: another user has several portfolios
        UUID userId = UUID.randomUUID();
        List<Portfolio> portfolios = withTransaction(() -> {
            List<Portfolio> list = IntStream.range(0, 5)
                .mapToObj(i -> mockPortfolio(userId))
                .toList();

            portfolioRepository.saveAll(list);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();
            return list;
        });

        // When: the authenticated user calls the endpoint
        PaginatedPortfolios response = given()
            .request()
            .contentType(JSON)
            .when()
            .queryParam("page", 0)
            .queryParam("page-size", 100)
            .get("/api/v1/shares/portfolios")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PaginatedPortfolios.class);

        // Then: the no portfolios are returned
        assertNotNull(response);

        assertEquals(0, response.getTotal());
        assertNotNull(response.getItems());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolio() {
        // Given: the user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            // Given: a share index exists
            ShareIndex shareIndex = shareIndexRepository.save(mockShareIndex());

            // And: several share price records exist
            priceHistoryRepository.saveAll(mockPriceHistory(shareIndex,
                LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(2))
            );

            // And: the user has a portfolio
            UUID userId = UUID.fromString(userIdStr);
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the portfolio holds the share
            Holding holding = newPortfolio.add(shareIndex);

            // And: the holding has several dealings
            holding.buy(LocalDate.now().minusDays(100), 100, BigDecimal.valueOf(111.11));
            holding.buy(LocalDate.now().minusDays(90), 200, BigDecimal.valueOf(222.22));
            holding.buy(LocalDate.now().minusDays(80), 300, BigDecimal.valueOf(333.33));

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            return newPortfolio;
        });

        // When: the user calls the endpoint
        PortfolioResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("portfolioId", portfolio.getId())
            .get("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract()
            .as(PortfolioResponse.class);

        // Then: the portfolio and its holdings are returned
        assertNotNull(response);

        assertEquals(portfolio.getId(), response.getId());
        assertEquals(portfolio.getName(), response.getName());

        assertNotNull(response.getHoldings());
        assertEquals(portfolio.getHoldings().size(), response.getHoldings().size());

        portfolio.getHoldings().forEach(expected -> {
            HoldingResponse actual = response.getHoldings().stream()
                .filter(h -> expected.getId().equals(h.getId()))
                .findFirst().orElse(null);

            assertNotNull(actual);
            assertNotNull(actual.getDealings());
            assertEquals(expected.getDealings().size(), actual.getDealings().size());
        });
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testGetPortfolio_WrongUser() {
        // Given: another user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            UUID userId = UUID.randomUUID();
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            return newPortfolio;
        });

        // When: the authenticated user calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("portfolioId", portfolio.getId())
            .get("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // Then: the response indicates the error
        assertNotNull(response);

        assertEquals(ErrorSeverity.INFO, response.getSeverity());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());

        ServiceError serviceError = response.getErrors().get(0);
        assertEquals("ENTITY_NOT_FOUND", serviceError.getMessageId());

        assertNotNull(serviceError.getContextAttributes());
        assertEquals("Portfolio", serviceError.getContextAttributes().get("entity-type"));
        assertEquals(portfolio.getId().toString(), serviceError.getContextAttributes().get("entity-id"));
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeletePortfolio() {
        AtomicLong originalCount = new AtomicLong();

        // Given: the user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            originalCount.set(portfolioRepository.count());

            UUID userId = UUID.fromString(userIdStr);
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            // And: the count is incremented
            assertEquals(originalCount.get() + 1, portfolioRepository.count());
            return newPortfolio;
        });

        // When: the service is called
        given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("portfolioId", portfolio.getId())
            .delete("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(204);

        // Then: the portfolio count is as original
        assertEquals(originalCount.get(), portfolioRepository.count());
    }

    @Test
    @TestSecurity(user = userIdStr, roles = "user")
    public void testDeletePortfolio_WrongUser() {
        AtomicLong originalCount = new AtomicLong();

        // Given: the user has a portfolio
        Portfolio portfolio = withTransaction(() -> {
            originalCount.set(portfolioRepository.count());

            UUID userId = UUID.randomUUID();
            Portfolio newPortfolio = mockPortfolio(userId);

            // And: the whole portfolio is saved
            portfolioRepository.save(newPortfolio);
            portfolioRepository.flush();
            portfolioRepository.getEntityManager().clear();

            // And: the count is incremented
            assertEquals(originalCount.get() + 1, portfolioRepository.count());
            return newPortfolio;
        });

        // When: the authenticated user calls the endpoint
        ServiceErrorResponse response = given()
            .request()
            .contentType(JSON)
            .when()
            .pathParam("portfolioId", portfolio.getId())
            .delete("/api/v1/shares/portfolios/{portfolioId}")
            .then()
            .statusCode(404)
            .contentType(JSON)
            .extract()
            .as(ServiceErrorResponse.class);

        // Then: the response indicates the error
        assertNotNull(response);

        assertEquals(ErrorSeverity.INFO, response.getSeverity());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());

        ServiceError serviceError = response.getErrors().get(0);
        assertEquals("ENTITY_NOT_FOUND", serviceError.getMessageId());

        assertNotNull(serviceError.getContextAttributes());
        assertEquals("Portfolio", serviceError.getContextAttributes().get("entity-type"));
        assertEquals(portfolio.getId().toString(), serviceError.getContextAttributes().get("entity-id"));

        // And: no portfolio is deleted
        assertEquals(originalCount.get() + 1, portfolioRepository.count());
    }

    @Transactional
    public <T> T withTransaction(Supplier<T> supplier) {
        return supplier.get();
    }
}
