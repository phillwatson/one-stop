package com.hillayes.rail.services;

import com.hillayes.commons.caching.Cache;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.model.Account;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionList;
import com.hillayes.rail.repository.RailAccountRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RailAccountService {
    @Inject
    @RestClient
    RailAccountRepository railAccountRepository;

    @Inject
    ServiceConfiguration config;

    private Cache<UUID, Map<String,Object>> accountDetailCache;

    @PostConstruct
    public void init() {
        accountDetailCache = new Cache<>(config.caches().accountDetails());
    }

    public Account get(UUID id) {
        return railAccountRepository.get(id);
    }

    public AccountBalanceList balances(UUID id) {
        return railAccountRepository.balances(id);
    }

    public Map<String,Object> details(UUID id) {
        return accountDetailCache.getValueOrCall(id, () ->
            railAccountRepository.details(id)
        );
    }

    public TransactionList transactions(UUID id,
                                        LocalDate dateFrom,
                                        LocalDate dateTo) {
        return railAccountRepository.transactions(id, dateFrom, dateTo);
    }
}
