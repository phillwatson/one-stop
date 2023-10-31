package com.hillayes.rail.service;

import com.hillayes.commons.caching.Cache;
import com.hillayes.nordigen.api.AccountApi;
import com.hillayes.nordigen.model.*;
import com.hillayes.rail.config.ServiceConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RailAccountService extends AbstractRailService {
    @Inject
    @RestClient
    AccountApi accountApi;

    @Inject
    ServiceConfiguration config;

    private Cache<String, Map<String, Object>> accountDetailCache;

    @PostConstruct
    public void init() {
        accountDetailCache = new Cache<>(config.caches().accountDetails());
    }

    public Optional<AccountSummary> get(String accountId) {
        log.debug("Retrieving account summary [id: {}]", accountId);
        try {
            return Optional.ofNullable(accountApi.get(accountId));
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                // indicate account-not-found
                log.info("Failed to retrieve rail account [accountId: {}]", accountId);
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Returns the balance records for the identified account. If no balance records
     * are found, an empty list is returned.
     * If the account cannot be found, an empty value is returned.
     *
     * @param accountId the account identifier.
     * @return an optional list of balance records.
     */
    public Optional<List<Balance>> balances(String accountId) {
        log.debug("Retrieving account balances [id: {}]", accountId);
        try {
            AccountBalanceList balanceList = accountApi.balances(accountId);
            if ((balanceList == null) || (balanceList.balances == null)) {
                return Optional.of(List.of());
            }
            return Optional.of(balanceList.balances);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                // indicate account-not-found
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<Map<String, Object>> details(String accountId) {
        log.debug("Retrieving account detail [id: {}]", accountId);
        try {
            Map<String, Object> detail = accountDetailCache.getValueOrCall(accountId, () ->
                accountApi.details(accountId)
            );
            return Optional.of(detail);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                // indicate account-not-found
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Returns the transactions for the identified account for the given period.
     * If the account cannot be found, the result will be an empty optional.
     *
     * @param accountId the rail account's unique identifier.
     * @param dateFrom the date of the start of the period to be searched, inclusive.
     * @param dateTo the date of the end of the period to be searched, inclusive.
     * @return the optional list of booked and pending transactions.
     */
    public Optional<TransactionList> transactions(String accountId,
                                                  LocalDate dateFrom,
                                                  LocalDate dateTo) {
        log.debug("Retrieving account transactions [id: {}, from: {}, to: {}]", accountId, dateFrom, dateTo);
        try {
            // call the rail service endpoint
            TransactionsResponse response = accountApi.transactions(accountId, dateFrom, dateTo);

            // if the result is null
            if ((response == null) || (response.transactions == null)) {
                // return a non-empty optional result - empty would indicate account-not-found
                return Optional.of(TransactionList.NULL_LIST);
            }
            return Optional.of(response.transactions);
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                // indicate account-not-found
                return Optional.empty();
            }
            throw e;
        }
    }
}
