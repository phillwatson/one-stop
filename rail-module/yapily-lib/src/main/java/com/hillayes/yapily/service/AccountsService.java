package com.hillayes.yapily.service;

import com.hillayes.yapily.api.AccountsApi;
import com.hillayes.yapily.model.Account;
import com.hillayes.yapily.model.AccountAuthorisationRequest;
import com.hillayes.yapily.model.ApiResponseOfAccountAuthorisationResponse;
import com.hillayes.yapily.model.Transaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class AccountsService extends AbstractRailService {
    private static final int TRANSACTION_PAGE_SIZE = 999;

    @Inject
    @RestClient
    AccountsApi accountsApi;

    public ApiResponseOfAccountAuthorisationResponse createAccountAuthorisation(AccountAuthorisationRequest request) {
        log.debug("Creating account authorisation [institution: {}]", request.getInstitutionId());
        return accountsApi.createAccountAuthorisation(request);
    }

    public List<Account> getAccounts(String consentToken) {
        log.debug("Retrieving accounts");
        return accountsApi.getAccounts(consentToken).getData();
    }

    public Optional<Account> getAccount(String consentToken, String accountId) {
        log.debug("Retrieving account [accountId: {}]", accountId);
        try {
            return Optional.ofNullable(accountsApi.getAccount(consentToken, accountId).getData());
        } catch (WebApplicationException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public List<Transaction> getAccountTransactions(String consentToken, String accountId, LocalDate from) {
        log.debug("Retrieving account transactions [accountId: {}, from: {}]", accountId, from);

        List<Transaction> result = new ArrayList<>();
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();
        int offset = 0;
        List<Transaction> data;
        do {
            data = accountsApi.getTransactions(consentToken, accountId,
                start, now, offset, TRANSACTION_PAGE_SIZE, null, "date").getData();
            if ((data == null) || (data.isEmpty())) {
                break;
            }
            result.addAll(data);
            offset += TRANSACTION_PAGE_SIZE;
        } while (data.size() == TRANSACTION_PAGE_SIZE);
        return result;
    }
}
