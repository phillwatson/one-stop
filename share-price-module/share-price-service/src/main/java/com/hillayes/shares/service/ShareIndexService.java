package com.hillayes.shares.service;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.domain.ShareIndexResolution;
import com.hillayes.shares.domain.SharePriceHistory;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.repository.SharePriceHistoryRepository;
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
    private final SharePriceHistoryRepository sharePriceHistoryRepository;
    private final ShareProviderFactory providerFactory;

    @Transactional
    public int refreshSharePrices(UUID shareIndexId) {
        Optional<ShareIndex> shareIndexOptional = shareIndexRepository.findByIdOptional(shareIndexId);
        if (shareIndexOptional.isEmpty()) {
            log.warn("Failed to locate share index [id: {}]", shareIndexId);
            return 0;
        }

        return shareIndexOptional.map(shareIndex -> {
            ShareProviderApi provider = providerFactory.get(shareIndex.getProvider());

            LocalDate fromDate = sharePriceHistoryRepository.getMostRecent(shareIndex)
                .map(price -> price.getId().getDate())
                .orElse(LocalDate.now().minusDays(provider.getMaxHistory()));

            LocalDate toDate = LocalDate.now().minusDays(1);
            Optional<List<PriceData>> prices = provider.getPrices(shareIndex.getIsin(), fromDate, toDate);
            if (prices.isEmpty()) {
                log.warn("Provider failed to return prices [isin: {}, name: {}]", shareIndex.getIsin(), shareIndex.getName());
                return 0;
            }

            prices.map(p -> p.stream()
                    .map(priceData -> marshal(shareIndex, priceData)).toList()
                )
                .ifPresent(sharePriceHistoryRepository::saveBatch);
            return prices.get().size();
        }).orElse(0);
    }

    private SharePriceHistory marshal(ShareIndex shareIndex, PriceData priceData) {
        return SharePriceHistory.builder()
            .id(SharePriceHistory.PrimaryKey.builder()
                .shareIndexId(shareIndex.getId())
                .resolution(ShareIndexResolution.DAILY)
                .date(priceData.date())
                .build())
            .open(priceData.open())
            .low(priceData.low())
            .high(priceData.high())
            .close(priceData.close())
            .build();
    }
}
