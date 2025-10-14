package com.hillayes.alphavantage.api;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.api.domain.PriceData;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The bridge that provides access to the Alpha Vantage share price data.
 */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageProvider implements ShareProviderApi {

    @Override
    public ShareProvider getProviderId() {
        return ShareProvider.ALPHA_ADVANTAGE;
    }

    @Override
    public int getMaxHistory() {
        return 100;
    }

    @Override
    public Optional<List<PriceData>> getPrices(String stockIsin, LocalDate startDate, LocalDate endDate) {
        return Optional.empty();
    }
}
