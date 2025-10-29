package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.errors.DuplicatePortfolioException;
import com.hillayes.shares.errors.SharesErrorCodes;
import com.hillayes.shares.repository.PortfolioRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.shares.utils.TestData.mockPortfolio;
import static com.hillayes.shares.utils.TestData.randomStrings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestTransaction
public class PortfolioServiceTest {
    @InjectSpy
    private PortfolioRepository portfolioRepository;

    @Inject
    private PortfolioService portfolioService;

    @Test
    public void testCreatePortfolio_Success() {
        // Given: a new portfolio record
        UUID userId = UUID.randomUUID();
        String portfolioName = randomStrings.nextAlphanumeric(30);

        // When: the service is called
        Portfolio result = portfolioService.createPortfolio(userId, portfolioName);

        // Then: the repository saves the portfolio
        verify(portfolioRepository).saveAndFlush(any());

        // And: the saved portfolio is returned
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(portfolioName, result.getName());
    }

    @Test
    public void testCreatePortfolio_DuplicateName_DifferentUser() {
        // Given: a collection of portfolio exist
        List<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // And: a new portfolio to be created with the same name as another
        UUID userId = UUID.randomUUID();
        String duplicateName = portfolios.get(0).getName();

        // When: the service is called
        Portfolio result = portfolioService.createPortfolio(userId, duplicateName);

        // Then: the repository saves the portfolio
        verify(portfolioRepository).saveAndFlush(any());

        // And: the saved portfolio is returned
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(duplicateName, result.getName());
    }

    @Test
    public void testCreatePortfolio_DuplicateName_SameUser() {
        // Given: a collection of portfolio exist
        List<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // And: a new portfolio to be created with the same userId and name as another
        Portfolio existing = portfolios.get(0);

        // When: the service is called
        DuplicatePortfolioException exception = assertThrows(DuplicatePortfolioException.class, () -> {
            portfolioService.createPortfolio(existing.getUserId(), existing.getName());
            portfolioRepository.flush();
        });

        // Then: the repository saves the portfolio
        verify(portfolioRepository).saveAndFlush(any());

        // And: the exception is as expected
        assertEquals(SharesErrorCodes.DUPLICATE_PORTFOLIO, exception.getErrorCode());
        assertEquals("A portfolio of that name already exists.", exception.getMessage());
        assertEquals(existing.getName(), exception.getParameter("portfolio-name"));
    }

    @Test
    public void testUpdatePortfolio_Found() {
        // Given: a collection of portfolio exist
        Iterable<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // When: the service is called for each portfolio
        portfolios.forEach(portfolio -> {
            String newName = randomStrings.nextAlphanumeric(30);
            Optional<Portfolio> result = portfolioService
                .updatePortfolio(portfolio.getUserId(), portfolio.getId(), newName);

            // Then: the repository is called to find the portfolio
            verify(portfolioRepository).findByIdOptional(portfolio.getId());

            // And: a call is made to save the updated portfolio
            verify(portfolioRepository).saveAndFlush(portfolio);

            // And: the updated portfolio is returned
            assertNotNull(result);
            assertTrue(result.isPresent());
            assertEquals(portfolio.getId(), result.get().getId());
            assertEquals(portfolio.getUserId(), result.get().getUserId());
            assertEquals(newName, result.get().getName());
        });
    }

    @Test
    public void testUpdatePortfolio_NotFound() {
        // Given: a collection of portfolio exist
        portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // Given: an invalid portfolio ID
        UUID portfolioId = UUID.randomUUID();

        // When: the service is called with the wrong user ID
        String newName = randomStrings.nextAlphanumeric(30);
        Optional<Portfolio> result = portfolioService.updatePortfolio(UUID.randomUUID(), portfolioId, newName);

        // Then: the repository is called to find the portfolio
        verify(portfolioRepository).findByIdOptional(portfolioId);

        // And: no call is made to save the updated portfolio
        verify(portfolioRepository, never()).saveAndFlush(any());

        // And: NO portfolio is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdatePortfolio_DuplicateName_DifferentUser() {
        // Given: a collection of portfolio exist
        List<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // And: a portfolio to be updated with the same name as another
        Portfolio otherUserPortfolio = portfolios.get(0);
        Portfolio thisUserPortfolio = portfolios.get(1);

        // When: the service is called
        Optional<Portfolio> result = portfolioService.updatePortfolio(
            thisUserPortfolio.getUserId(),
            thisUserPortfolio.getId(),
            otherUserPortfolio.getName()
        );

        // Then: the repository saves the portfolio
        verify(portfolioRepository).saveAndFlush(any());

        // And: the updated portfolio is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(thisUserPortfolio.getId(), result.get().getId());
        assertEquals(thisUserPortfolio.getUserId(), result.get().getUserId());
        assertEquals(otherUserPortfolio.getName(), result.get().getName());
    }

    @Test
    public void testUpdatePortfolio_DuplicateName_SameUser() {
        // Given: a collection of portfolio exist for the same user
        UUID userId = UUID.randomUUID();
        List<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(userId)
            ).toList()
        );
        portfolioRepository.flush();

        // And: a portfolio to be updated with the same name as another
        Portfolio existing = portfolios.get(0);
        Portfolio update = portfolios.get(1);

        // When: the service is called to update one portfolio with the name of another
        DuplicatePortfolioException exception = assertThrows(DuplicatePortfolioException.class, () -> {
            portfolioService.updatePortfolio(userId, update.getId(), existing.getName());
        });

        // Then: the repository saves the portfolio
        verify(portfolioRepository).saveAndFlush(any());

        // And: the exception is as expected
        assertEquals(SharesErrorCodes.DUPLICATE_PORTFOLIO, exception.getErrorCode());
        assertEquals("A portfolio of that name already exists.", exception.getMessage());
        assertEquals(existing.getName(), exception.getParameter("portfolio-name"));
    }

    @Test
    public void testGetPortfolio_Found() {
        // Given: a collection of portfolio exist
        Iterable<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // When: the service is called for each portfolio
        portfolios.forEach(portfolio -> {
            Optional<Portfolio> result = portfolioService.getPortfolio(portfolio.getUserId(), portfolio.getId());

            // Then: the repository is called
            verify(portfolioRepository).findByIdOptional(portfolio.getId());

            // And: the portfolio is returned
            assertNotNull(result);
            assertTrue(result.isPresent());
            assertEquals(portfolio.getId(), result.get().getId());
            assertEquals(portfolio.getUserId(), result.get().getUserId());
            assertEquals(portfolio.getName(), result.get().getName());
        });
    }

    @Test
    public void testGetPortfolio_NotFound() {
        // Given: a collection of portfolio exist
        portfolioRepository.saveAll(
            IntStream.range(0, 10).mapToObj(i ->
                mockPortfolio(UUID.randomUUID())
            ).toList()
        );
        portfolioRepository.flush();

        // Given: an invalid portfolio ID
        UUID portfolioId = UUID.randomUUID();

        // When: the service is called with the wrong user id
        Optional<Portfolio> result = portfolioService.getPortfolio(UUID.randomUUID(), portfolioId);

        // Then: the repository is called
        verify(portfolioRepository).findByIdOptional(portfolioId);

        // And: NO portfolio is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListPortfolios_Exists() {
        // Given: a collection of portfolio exist for the same user
        UUID userId = UUID.randomUUID();
        portfolioRepository.saveAll(
            IntStream.range(0, 20)
                .mapToObj(i -> mockPortfolio(userId))
                .toList()
        );

        // And: some for other users
        portfolioRepository.saveAll(
            IntStream.range(0, 10)
                .mapToObj(i -> mockPortfolio(UUID.randomUUID()))
                .toList()
        );
        portfolioRepository.flush();

        // When: the service is called for the given user
        Page<Portfolio> page = portfolioService.listPortfolios(userId, 1, 5);

        // Then: the repository is called to retrieve the page
        verify(portfolioRepository).getUsersPortfolios(userId, 1, 5);

        // And: the selected page is returned
        assertNotNull(page);
        assertEquals(1, page.getPageIndex());
        assertEquals(5, page.getPageSize());

        // And: only the identified user's portfolios are included
        page.forEach(portfolio -> assertEquals(userId, portfolio.getUserId()));
    }

    @Test
    public void testListPortfolios_NonExist() {
        // Given: an identified user
        UUID userId = UUID.randomUUID();

        // And: portfolios exist for other users
        portfolioRepository.saveAll(
            IntStream.range(0, 10)
                .mapToObj(i -> mockPortfolio(UUID.randomUUID()))
                .toList()
        );
        portfolioRepository.flush();

        // When: the service is called for the given user
        Page<Portfolio> page = portfolioService.listPortfolios(userId, 1, 5);

        // Then: the repository is called to retrieve the page
        verify(portfolioRepository).getUsersPortfolios(userId, 1, 5);

        // And: the selected page is returned
        assertNotNull(page);
        assertEquals(1, page.getPageIndex());
        assertEquals(5, page.getPageSize());

        // And: NO portfolios are included
        assertTrue(page.isEmpty());
    }

    @Test
    public void testDelete_Existing() {
        // Given: a collection of portfolio exist
        Iterable<Portfolio> portfolios = portfolioRepository.saveAll(
            IntStream.range(0, 20)
                .mapToObj(i -> mockPortfolio(UUID.randomUUID()))
                .toList()
        );
        portfolioRepository.flush();

        // When: the service is called to delete each portfolio
        portfolios.forEach(portfolio ->  {
            Optional<Portfolio> deleted = portfolioService.deletePortfolio(portfolio.getUserId(), portfolio.getId());

            // Then: the repository is called to delete the portfolio
            verify(portfolioRepository).delete(portfolio);

            // And: the deleted portfolio is returned
            assertNotNull(deleted);
            assertTrue(deleted.isPresent());
            assertEquals(portfolio.getId(), deleted.get().getId());
        });
    }

    @Test
    public void testDelete_NonExisting() {
        // Given: a collection of portfolio exist
        portfolioRepository.saveAll(
            IntStream.range(0, 20)
                .mapToObj(i -> mockPortfolio(UUID.randomUUID()))
                .toList()
        );
        portfolioRepository.flush();

        // And: an invalid portfolio ID
        UUID portfolioId = UUID.randomUUID();

        // When: the service is called with the wrong user id
        Optional<Portfolio> deleted = portfolioService.deletePortfolio(UUID.randomUUID(), portfolioId);

        // Then: the repository is NOT called to delete the non-existing portfolio
        verify(portfolioRepository, never()).delete(any());

        // And: no portfolio is returned
        assertNotNull(deleted);
        assertTrue(deleted.isEmpty());
    }
}
