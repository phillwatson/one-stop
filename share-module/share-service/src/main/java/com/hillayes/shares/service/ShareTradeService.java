package com.hillayes.shares.service;

import com.hillayes.exception.common.NotFoundException;
import com.hillayes.shares.domain.Holding;
import com.hillayes.shares.domain.Portfolio;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.SaleExceedsHoldingException;
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
     * At least one of shareIsin or tickerSymbol must be provided.
     *
     * @param userId the user making the trade.
     * @param portfolioId the portfolio to which the trade is to be recorded.
     * @param dateExecuted the date on which the trade was executed.
     * @param shareIdentity the ISIN and/or ticker symbol identifying the stock being traded.
     * @param quantity the number of shares traded (negative = sell, positive = buy)
     * @param pricePerShare the price at which the shares were traded.
     * @return the updated holding.
     */
    @Transactional
    public Holding createShareTrade(UUID userId, UUID portfolioId,
                                    LocalDate dateExecuted,
                                    ShareIndex.ShareIdentity shareIdentity,
                                    int quantity,
                                    BigDecimal pricePerShare) {
        log.info("Creating a share trade [portfolioId: {}, date: {}, identity: {}, quantity: {}, price: {}]",
            portfolioId, dateExecuted, shareIdentity, quantity, pricePerShare);

        if (quantity == 0) {
            throw new ZeroTradeQuantityException(shareIdentity);
        }

        Portfolio portfolio = portfolioRepository.findByIdOptional(portfolioId)
            .filter(p -> userId.equals(p.getUserId()))
            .orElseThrow(() -> new NotFoundException("Portfolio", portfolioId));

        ShareIndex shareIndex = shareIndexService.getShareIndex(shareIdentity)
            .orElseThrow(() -> new NotFoundException("ShareIndex", shareIdentity));

        Holding holding = portfolio.get(shareIndex)
            .orElseGet(() -> portfolio.add(shareIndex));

        if (quantity > 0)
            holding.buy(dateExecuted, quantity, pricePerShare);
        else if (-quantity > holding.getQuantity())
            throw new SaleExceedsHoldingException(shareIdentity, quantity, holding.getQuantity());
        else
            holding.sell(dateExecuted, quantity, pricePerShare);

        holdingRepository.saveAndFlush(holding);
        return holding;
    }
}
