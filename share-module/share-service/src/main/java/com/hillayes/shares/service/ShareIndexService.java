package com.hillayes.shares.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.shares.config.ShareProviderFactory;
import com.hillayes.shares.domain.ShareIndex;
import com.hillayes.shares.errors.DuplicateShareIndexException;
import com.hillayes.shares.repository.ShareIndexRepository;
import com.hillayes.shares.scheduled.PollShareIndexAdhocTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShareIndexService {
    private final ShareIndexRepository shareIndexRepository;
    private final PollShareIndexAdhocTask pollShareIndexAdhocTask;
    private final ShareProviderFactory providerFactory;

    public Optional<ShareIndex> getShareIndex(UUID shareIndexId) {
        return shareIndexRepository.findByIdOptional(shareIndexId);
    }

    public Optional<ShareIndex> getShareIndex(ShareIndex.ShareIdentity identity) {
        return shareIndexRepository.findByIdentity(identity);
    }

    public Collection<ShareIndex> registerShareIndices(Collection<ShareIndex.ShareIdentity> indices) {
        if ((indices == null) || (indices.isEmpty()))
            return List.of();

        return indices.stream()
            .map(this::registerShareIndex)
            .toList();
    }

    public ShareIndex registerShareIndex(ShareIndex.ShareIdentity identity) {
        log.info("Creating new ShareIndex [identity: {}]", identity);
        try {
            ShareIndex shareIndex = providerFactory.getAll()
                .map(provider -> {
                    try {
                        return provider.getShareInfo(identity.getIsin(), identity.getTickerSymbol())
                            .map(info -> ShareIndex.builder()
                                .identity(ShareIndex.ShareIdentity.builder()
                                    .isin(info.getIsin())
                                    .tickerSymbol(info.getTickerSymbol())
                                    .build())
                                .name(info.getName())
                                .currency(info.getCurrency())
                                .provider(provider.getProviderId())
                                .build()
                            );
                    } catch (Exception e) {
                        log.warn("Share Provider failure.", e);
                        return Optional.<ShareIndex>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("ShareIndex", identity));

            ShareIndex index = shareIndexRepository.saveAndFlush(shareIndex);

            // queue task to retrieve share prices
            pollShareIndexAdhocTask.queueTask(index.getId());

            log.debug("Created ShareIndex [identity: {}, id: {}]", index.getIdentity(), index.getId());
            return index;
        } catch (ConstraintViolationException e) {
            throw new DuplicateShareIndexException(identity, e);
        }
    }

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
