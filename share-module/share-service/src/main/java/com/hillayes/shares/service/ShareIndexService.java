package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.repository.ShareIndexRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ShareIndexService {
    private final ShareIndexRepository shareIndexRepository;

    @Transactional
    public Optional<ShareIndex> getShareIndex(UUID shareIndexId) {
        return shareIndexRepository.findByIdOptional(shareIndexId);
    }

    @Transactional
    public ShareIndex registerShareIndex(String isin,
                                         String name,
                                         Currency currency,
                                         ShareProvider provider) {
        log.info("Creating new ShareIndex [isin: {}]", isin);
        ShareIndex index = ShareIndex.builder()
            .isin(isin)
            .name(name)
            .currency(currency)
            .provider(provider)
            .build();

        shareIndexRepository.save(index);
        log.debug("Created ShareIndex [isin: {}, id: {}]", index.getIsin(), index.getId());
        return index;
    }

    @Transactional
    public Page<ShareIndex> listShareIndexes(int pageIndex, int pageSize) {
        log.info("Listing share indexes [page: {}, pageSize: {}]", pageIndex, pageSize);

        Page<ShareIndex> result = shareIndexRepository.listAll(pageIndex, pageSize);

        log.debug("Listing share indexes [page: {}, pageSize: {}, size: {}, totalCount: {}]",
            pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        return result;
    }
}
