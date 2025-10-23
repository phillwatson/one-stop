package com.hillayes.shares.api;

import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Represents the interface made available by all Share Data Providers that we
 * intend to support. This is intended to be agnostic of the underlying provider,
 * and* implementations will use the Bridge pattern to map the Share Data Provider's
 * functionality to this interface.
 */
public interface ShareProviderApi {
    /**
     * Used when we have a collection of ShareProviderApi implementations, and we
     * want to select the correct instance based on a given ShareProvider value.
     * <p>
     * We inject all instances using the class Instance<ShareProviderApi>, and use
     * this method to identify the appropriate instance. For example;
     * <pre>
     *     \@Inject \@Any
     *     jakarta.enterprise.inject.Instance<ShareProviderApi> shareProviderApis;
     * </pre>
     *
     * @param shareProvider the ShareProvider value that identifies the implementation.
     * @return true if this instance supports the given ShareProvider.
     */
    public default boolean isFor(ShareProvider shareProvider) {
        return getProviderId() == shareProvider;
    }

    /**
     * Returns the ShareProvider value that identifies the underlying provider.
     */
    public ShareProvider getProviderId();

    /**
     * Returns the maximum number of days for which this provider can retrieve
     * historical price information.
     */
    public int getMaxHistory();

    /**
     * Returns the share price movements between the given dates, inclusive.
     * Or an empty value if the provider does not support the given ISIN.
     *
     * @param stockIsin the International Securities Identification Number (ISIN)
     *                  for the stock in question.
     * @param startDate the start of the date range; inclusive.
     * @param endDate the end of the date range; inclusive.
     * @return the collection of share price movements; in ascending date order.
     */
    public Optional<List<PriceData>> getPrices(String stockIsin, LocalDate startDate, LocalDate endDate);
}
