package com.hillayes.shares.ft.service;

import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.ft.client.MarketsClient;
import com.hillayes.shares.api.errors.IsinNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PriceLookupService {
    private final IsinLookupService isinLookupService;
    private final MarketsClient marketsClient;

    public List<PriceData> getPrices(String stockIsin, LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving share prices [isin: {}, startDate: {}, endDate: {}]", stockIsin, startDate, endDate);
        String issueId = isinLookupService.lookupIssueId(stockIsin)
            .orElseThrow(() -> new IsinNotFoundException(ShareProvider.FT_MARKET_DATA, stockIsin));

        return marketsClient.getPrices(issueId, startDate, endDate);
    }
}
