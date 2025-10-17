package com.hillayes.shares.service;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.PriceHistory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.SharePriceResolution;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.PriceHistoryRepository;
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
public class ShareIndexService {
    private final ShareIndexRepository shareIndexRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ShareProviderFactory providerFactory;

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
            .orElse(LocalDate.now().minusDays(provider.getMaxHistory()));
        LocalDate toDate = LocalDate.now().minusDays(1);

        // if we don't need to refresh the prices
        if (! fromDate.isBefore(toDate)) {
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
            .build();
    }
}
