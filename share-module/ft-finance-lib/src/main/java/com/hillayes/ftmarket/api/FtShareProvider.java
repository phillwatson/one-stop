package com.hillayes.ftmarket.api;

import com.hillayes.commons.Strings;
import com.hillayes.ftmarket.api.service.IsinLookupService;
import com.hillayes.ftmarket.api.service.PriceLookupService;
import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareInfo;
import com.hillayes.shares.api.domain.ShareProvider;
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
    private final IsinLookupService isinLookupService;
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
    public Optional<ShareInfo> getShareInfo(String isin, String tickerSymbol) {
        return isinLookupService.lookupIssueId(isin)
            .or(() -> isinLookupService.lookupIssueId(tickerSymbol))
            .map(lookup -> new ShareInfo(
                isin,
                tickerSymbol,
                lookup.getName(),
                lookup.getCurrencyCode()
            ));
    }

    @Override
    public Optional<List<PriceData>> getPrices(String stockIsin, String tickerSymbol,
                                               LocalDate startDate, LocalDate endDate) {
        return priceLookupService.getPrices(stockIsin, startDate, endDate)
            .or(() -> priceLookupService.getPrices(tickerSymbol, startDate, endDate));
    }
}
