package com.hillayes.rail.services;

import com.hillayes.rail.model.Account;
import com.hillayes.rail.model.AccountBalanceList;
import com.hillayes.rail.model.TransactionList;
import com.hillayes.rail.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AccountService {
    @Inject
    @RestClient
    AccountRepository accountRepository;

    public Account get(UUID id) {
        return accountRepository.get(id);
    }

    public AccountBalanceList balances(UUID id) {
        return accountRepository.balances(id);
    }

    public Map<String,Object> details(UUID id) {
        return accountRepository.details(id);
    }

    public TransactionList transactions(UUID id,
                                        LocalDate dateFrom,
                                        LocalDate dateTo) {
        return accountRepository.transactions(id, dateFrom, dateTo);
    }
}
