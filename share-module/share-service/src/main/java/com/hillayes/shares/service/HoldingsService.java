package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.repository.HoldingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class HoldingsService {
    private final HoldingRepository holdingRepository;
    private final PortfolioService portfolioService;

    public Optional<Holding> getHolding(UUID userId, UUID holdingId) {
        log.info("Getting holding [id: {}]", holdingId);
        return holdingRepository.findByIdOptional(holdingId)
            .filter(holding -> portfolioService.getPortfolio(userId, holding.getPortfolioId()).isPresent());
    }

    public Page<Holding> listHoldings(Portfolio portfolio, int pageIndex, int pageSize) {
        log.info("Listing portfolio holdings [ portfolio: {}, page: {}, pageSize: {}]",
            portfolio.getName(), pageIndex, pageSize);
        return holdingRepository.getHolding(portfolio, pageIndex, pageSize);
    }

}
