package com.hillayes.ftmarket.api.service;

import com.hillayes.commons.Strings;
import com.hillayes.ftmarket.api.client.MarketsClient;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import com.hillayes.ftmarket.api.repository.IsinIssueLookupRepository;
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
     * @param symbol the ticker or ISIN (International Securities Identification Number)
     * @return the FT Finance API issue ID for the given symbol
     */
    @Transactional
    public Optional<IsinIssueLookup> lookupIssueId(String symbol) {
        log.info("Looking up company issue-id [symbol: {}]", symbol);

        if (Strings.isBlank(symbol)) {
            return Optional.empty();
        }

        // lookup from local cache
        return lookupRepository.findByIsin(symbol)
            .or(() -> {
                // perform web-site lookup
                Optional<IsinIssueLookup> result = marketsClient.getIssueID(symbol);

                // if found - persist it for next time
                return result.map(lookupRepository::save);
            });
    }
}
