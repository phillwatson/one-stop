package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.DuplicateIsinException;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.scheduled.PollShareIndexAdhocTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

import java.util.*;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class ShareIndexService {
    private final ShareIndexRepository shareIndexRepository;
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask;

    @Transactional
    public Optional<ShareIndex> getShareIndex(UUID shareIndexId) {
        return shareIndexRepository.findByIdOptional(shareIndexId);
    }

    @Transactional
    public Optional<ShareIndex> getShareIndex(String shareIsin) {
        return shareIndexRepository.findByIsin(shareIsin);
    }

    @Transactional
    public Collection<ShareIndex> registerShareIndices(Collection<ShareIndex> indices) {
        if ((indices == null) || (indices.isEmpty()))
            return List.of();

        return indices.stream()
            .map(index ->
                registerShareIndex(
                    index.getIsin(), index.getName(),
                    index.getCurrency(), index.getProvider())
            )
            .toList();
    }

    @Transactional
    public ShareIndex registerShareIndex(String isin,
                                         String name,
                                         Currency currency,
                                         ShareProvider provider) {
        log.info("Creating new ShareIndex [isin: {}]", isin);
        try {
            ShareIndex index = shareIndexRepository.saveAndFlush(
                ShareIndex.builder()
                    .isin(isin)
                    .name(name)
                    .currency(currency)
                    .provider(provider)
                    .build()
            );

            // queue task to retrieve share prices
            pollShareIndexAdhocTask.queueTask(index.getId());

            log.debug("Created ShareIndex [isin: {}, id: {}]", index.getIsin(), index.getId());
            return index;
        } catch (ConstraintViolationException e) {
            throw new DuplicateIsinException(isin, e);
        }
    }

    @Transactional
    public Page<ShareIndex> listShareIndices(int pageIndex, int pageSize) {
        log.info("Listing share indices [page: {}, pageSize: {}]", pageIndex, pageSize);

        Page<ShareIndex> result = shareIndexRepository.listAll(pageIndex, pageSize);

        if (log.isDebugEnabled()) {
            log.debug("Listing share indices [page: {}, pageSize: {}, size: {}, totalCount: {}]",
                pageIndex, pageSize, result.getContentSize(), result.getTotalCount());
        }
        return result;
    }
}
