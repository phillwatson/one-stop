package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.repository.PriceHistoryRepository;
import com.hillayes.shares.repository.ShareIndexRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SharePriceService {
    private final ShareIndexRepository shareIndexRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ShareProviderFactory providerFactory;

    /**
     * Returns a page of PriceHistory records, in ascending date order, for
     * the given ShareIndex. The records will include only those within the given
     * date range.
     *
     * @param shareIndex the ShareIndex for which the prices are required.
     * @param fromDate the earliest date to be included in the page (inclusive).
     * @param toDate the latest date to be included in the page (exclusive).
     * @param pageIndex the (zero based) index of the page to be returned.
     * @param pageSize the size of the page, and the maximum number of records to be returned.
     * @return the page of records, or an empty page if no records are found.
     */
    public Page<PriceHistory> getPrices(ShareIndex shareIndex,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        int pageIndex,
                                        int pageSize) {
        log.info("Listing share prices [isin: {}, fromDate: {}, toDate: {}, page: {}, pageSize: {}",
            shareIndex.getIsin(), fromDate, toDate, pageIndex, pageSize);

        Page<PriceHistory> result =
            priceHistoryRepository.listPrices(
                shareIndex, SharePriceResolution.DAILY, fromDate, toDate, pageIndex, pageSize
            );

        log.debug("Listing share prices [isin: {}, fromDate: {}, toDate: {}, page: {}, pageSize: {}, size: {}, totalCount: {}]",
            shareIndex.getIsin(), fromDate, toDate, pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        return result;
    }

    /**
     * Returns the record of the most recent DAILY share price record for the given
     * ShareIndex.
     *
     * @param shareIndex the ShareIndex whose price history is to be searched.
     * @return the record of the most recent share price, or an empty result.
     */
    public Optional<PriceHistory> getMostRecentPrice(ShareIndex shareIndex) {
        return priceHistoryRepository.getMostRecent(shareIndex);
    }

    /**
     * Retrieves the latest share prices for the identified ShareIndex record.
     *
     * @param shareIndexId the ID of the ShareIndex record to be refreshed.
     * @return the number of price records found.
     */
    @Transactional
    public int refreshSharePrices(UUID shareIndexId) {
        log.info("Refreshing share prices [shareIndexId: {}]", shareIndexId);
        ShareIndex shareIndex = shareIndexRepository.findByIdOptional(shareIndexId)
            .orElse(null);

        if (shareIndex == null) {
            log.warn("Failed to locate share index [id: {}]", shareIndexId);
            return 0;
        }

        // which provider retrieved the original ShareIndex
        ShareProviderApi provider = providerFactory.get(shareIndex.getProvider());

        // from what date are the latest prices to be retrieved
        LocalDate fromDate = priceHistoryRepository.getMostRecent(shareIndex)
            .map(price -> price.getId().getDate())
            .orElseGet(() -> LocalDate.now().minusDays(provider.getMaxHistory()));
        LocalDate toDate = LocalDate.now().minusDays(1);

        // if we don't need to refresh the prices
        if (!fromDate.isBefore(toDate)) {
            log.debug("Share prices are up-to-date [isin: {}, most-recent: {}]",
                shareIndex.getIsin(), fromDate);
            return 0;
        }

        Optional<List<PriceData>> response = provider.getPrices(shareIndex.getIsin(), fromDate, toDate);
        if (response.isEmpty()) {
            log.warn("Provider failed locate share index [provider: {}, isin: {}, name: {}]",
                shareIndex.getProvider(), shareIndex.getIsin(), shareIndex.getName());
            return 0;
        }

        List<PriceData> prices = response.get();
        if (prices.isEmpty()) {
            log.warn("Provider returned no new prices [provider: {}, isin: {}, name: {}, from: {} ]",
                shareIndex.getProvider(), shareIndex.getIsin(), shareIndex.getName(), fromDate);
            return 0;
        }

        // persist the retrieved prices
        List<PriceHistory> history = prices.stream()
            .map(priceData -> marshal(shareIndex, priceData))
            .toList();
        priceHistoryRepository.saveBatch(history);

        log.debug("Retrieved and persisted latest share prices [ISIN: {}, count: {}]",
            shareIndex.getIsin(), prices.size());
        return prices.size();
    }

    private PriceHistory marshal(ShareIndex shareIndex, PriceData priceData) {
        return PriceHistory.builder()
            .id(PriceHistory.PrimaryKey.builder()
                .shareIndexId(shareIndex.getId())
                .resolution(SharePriceResolution.DAILY)
                .date(priceData.date())
                .build())
            .open(priceData.open())
            .low(priceData.low())
            .high(priceData.high())
            .close(priceData.close())
            .volume(priceData.volume())
            .build();
    }
}
