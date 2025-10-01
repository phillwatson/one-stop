package com.hillayes.shares.ft.service;

import com.hillayes.shares.ft.client.MarketsClient;
import com.hillayes.shares.ft.domain.IsinIssueLookup;
import com.hillayes.shares.ft.repository.IsinIssueLookupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class IsinLookupService {
    private final IsinIssueLookupRepository lookupRepository;
    private final MarketsClient marketsClient;

    /**
     * Returns the issue-id by which the FT Finance API identifies companies and
     * funds.
     *
     * @param stockIsin the company, or fund, International Securities Identification Number
     * @return the FT Finance API issue ID for the given ISIN
     */
    @Transactional
    public Optional<String> lookupIssueId(String stockIsin) {
        log.info("Looking up company issue-id [isin: {}]", stockIsin);
        // lookup from local cache
        return lookupRepository.findByIsin(stockIsin)
            .map(IsinIssueLookup::getIssueId)
            .or(() -> {
                // perform web-site lookup
                Optional<String> issueId = marketsClient.getIssueID(stockIsin);

                // if found - persist it for next time
                issueId.ifPresent(s ->
                    lookupRepository.save(IsinIssueLookup.builder()
                        .isin(stockIsin)
                        .issueId(s)
                        .build()));
                return issueId;
            });
    }
}
