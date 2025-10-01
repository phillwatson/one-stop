package com.hillayes.shares.ft;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.ft.service.PriceLookupService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

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
    public List<PriceData> getPrices(String stockIsin, LocalDate startDate, LocalDate endDate) {
        return priceLookupService.getPrices(stockIsin, startDate, endDate);
    }
}
