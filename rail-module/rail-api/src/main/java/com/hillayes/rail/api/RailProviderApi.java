package com.hillayes.rail.api;

import com.hillayes.rail.api.domain.*;
import jakarta.ws.rs.QueryParam;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RailProviderApi {
    public Optional<Institution> getInstitution(String id);

    public List<Institution> listInstitutions(@QueryParam("country") String countryCode);

    public Agreement register(String reference, Institution institution, URI callbackUri);

    public Optional<Agreement> getAgreement(String id);

    public void deleteAgreement(String id);

    public Optional<Account> getAccount(String id);

    public List<Balance> listBalances(String accountId, LocalDate dateFrom);

    public List<Transaction> listTransactions(String id,
                                              LocalDate dateFrom,
                                              LocalDate dateTo);
}
