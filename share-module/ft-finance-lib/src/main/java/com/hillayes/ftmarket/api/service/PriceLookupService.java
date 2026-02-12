package com.hillayes.ftmarket.api.service;

import com.hillayes.commons.Strings;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.ftmarket.api.client.MarketsClient;
import com.hillayes.shares.api.errors.IsinNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class PriceLookupService {
    private final IsinLookupService isinLookupService;
    private final MarketsClient marketsClient;

    public Optional<List<PriceData>> getPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving share prices [isin: {}, startDate: {}, endDate: {}]", symbol, startDate, endDate);

        if (Strings.isBlank(symbol)) {
            return Optional.empty();
        }

        IsinIssueLookup lookup = isinLookupService.lookupIssueId(symbol)
            .orElseThrow(() -> new IsinNotFoundException(ShareProvider.FT_MARKET_DATA, symbol));

        return Optional.ofNullable(marketsClient.getPrices(lookup.getIssueId(),
            lookup.getCurrencyUnits(), startDate, endDate));
    }
}
