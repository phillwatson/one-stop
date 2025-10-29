package com.hillayes.ftmarket.api;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.ftmarket.api.service.PriceLookupService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The bridge that provides access to the FT Market share price data.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class FtShareProvider implements ShareProviderApi {
    private final PriceLookupService priceLookupService;

    @Override
    public ShareProvider getProviderId() {
        return ShareProvider.FT_MARKET_DATA;
    }

    @Override
    public int getMaxHistory() {
        return 364;
    }

    @Override
    public Optional<List<PriceData>> getPrices(String stockIsin, LocalDate startDate, LocalDate endDate) {
        List<PriceData> result = priceLookupService.getPrices(stockIsin, startDate, endDate);
        if (result.isEmpty()) result = null;
        return Optional.ofNullable(result);
    }
}
