package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.errors.DuplicatePortfolioException;
import com.hillayes.shares.repository.PortfolioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;

    @Transactional
    public Portfolio createPortfolio(UUID userId, String portfolioName) {
        log.info("Creating portfolio [name: {}]", portfolioName);
        try {
            return portfolioRepository.saveAndFlush(Portfolio.builder()
                .userId(userId)
                .name(portfolioName)
                .build()
            );
        } catch (ConstraintViolationException e) {
            throw new DuplicatePortfolioException(portfolioName, e);
        }
    }

    @Transactional
    public Optional<Portfolio> updatePortfolio(UUID userId,
                                                 UUID portfolioId,
                                     String portfolioName) {
        log.info("Updating portfolio [name: {}]", portfolioName);
        try {
            Optional<Portfolio> portfolio = portfolioRepository.findByIdOptional(portfolioId)
                .filter(p -> userId.equals(p.getUserId()));

            if (portfolio.isEmpty()) {
                return Optional.empty();
            }

            return portfolio.map(p -> {
                p.setName(portfolioName);
                return portfolioRepository.saveAndFlush(p);
            });
        } catch (ConstraintViolationException e) {
            throw new DuplicatePortfolioException(portfolioName, e);
        }
    }

    @Transactional
    public Optional<Portfolio> getPortfolio(UUID userId, UUID portfolioId) {
        log.info("Getting portfolio [id: {}]", portfolioId);
        return portfolioRepository.findByIdOptional(portfolioId)
            .filter(portfolio -> userId.equals(portfolio.getUserId()));
    }

    @Transactional
    public Page<Portfolio> listPortfolios(UUID userId, int pageIndex, int pageSize) {
        log.info("Listing user's portfolios [userId: {}, page: {}, pageSize: {}]", userId, pageIndex, pageSize);
        return portfolioRepository.getUsersPortfolios(userId, pageIndex, pageSize);
    }

    @Transactional
    public Optional<Portfolio> deletePortfolio(UUID userId, UUID portfolioId) {
        log.info("Deleting portfolio [id: {}]", portfolioId);
        return portfolioRepository.findByIdOptional(portfolioId)
            .map(portfolio -> {
                portfolioRepository.delete(portfolio);

                log.debug("Deleted portfolio [id: {}, name: {}]", portfolio.getId(), portfolio.getName());
                return portfolio;
            });
    }
}
