package com.hillayes.shares.service;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.ZeroTradeQuantityException;
import com.hillayes.shares.repository.HoldingRepository;
import com.hillayes.shares.repository.PortfolioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ShareTradeService {
    private final ShareIndexService shareIndexService;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    /**
     * Registers a new trade in the identified share to be held within the
     * identified user's portfolio.
     *
     * @param userId the user making the trade.
     * @param portfolioId the portfolio to which the trade is to be recorded.
     * @param dateExecuted the date on which the trade was executed.
     * @param shareIsin the ISIN identifying the stock being traded.
     * @param quantity the number of shares traded (negative = sell, positive = buy)
     * @param pricePerShare the price at which the shares were traded.
     * @return the updated holding.
     */
    @Transactional
    public Holding createShareTrade(UUID userId, UUID portfolioId,
                                    LocalDate dateExecuted,
                                    String shareIsin,
                                    int quantity,
                                    BigDecimal pricePerShare) {
        log.info("Creating a share trade [portfolioId: {}, date: {}, isin: {}, quantity: {}, price: {}]",
            portfolioId, dateExecuted, shareIsin, quantity, pricePerShare);

        if (quantity == 0) {
            throw new ZeroTradeQuantityException(shareIsin);
        }

        Portfolio portfolio = portfolioRepository.findByIdOptional(portfolioId)
            .filter(p -> userId.equals(p.getUserId()))
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        ShareIndex shareIndex = shareIndexService.getShareIndex(shareIsin)
            .orElseThrow(() -> new NotFoundException("ShareIndex", shareIsin));

        Holding holding = portfolio.get(shareIndex)
            .orElseGet(() -> portfolio.add(shareIndex));

        if (quantity > 0)
            holding.buy(dateExecuted, quantity, pricePerShare);
        else
            holding.sell(dateExecuted, quantity, pricePerShare);

        holdingRepository.saveAndFlush(holding);
        return holding;
    }
}
