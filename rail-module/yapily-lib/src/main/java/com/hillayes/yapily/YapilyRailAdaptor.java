package com.hillayes.yapily;

import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class YapilyRailAdaptor implements RailProviderApi {
    @Override
    public boolean isFor(RailProvider railProvider) {
        return railProvider == RailProvider.YAPILY;
    }

    @Override
    public Optional<RailInstitution> getInstitution(String id) {
        log.debug("Getting institution [id: {}]", id);
        return Optional.empty();
    }

    @Override
    public List<RailInstitution> listInstitutions(String countryCode, boolean paymentsEnabled) {
        log.debug("Listing institutions [countryCode: {}, paymentsEnabled: {}]", countryCode, paymentsEnabled);
        return List.of();
    }

    @Override
    public RailAgreement register(RailInstitution institution, URI callbackUri, String reference) {
        log.debug("Requesting agreement [reference: {}, institutionId: {}]", reference, institution.getId());
        return null;
    }

    @Override
    public Optional<RailAgreement> getAgreement(String id) {
        log.debug("Getting agreement [id: {}]", id);
        return Optional.empty();
    }

    @Override
    public boolean deleteAgreement(String id) {
        log.debug("Deleting agreement [id: {}]", id);
        return true;
    }

    @Override
    public Optional<RailAccount> getAccount(String id) {
        log.debug("Getting account [id: {}]", id);
        return Optional.empty();
    }

    @Override
    public List<RailBalance> listBalances(String accountId, LocalDate dateFrom) {
        log.debug("Listing balances [accountId: {}, from: {}]", accountId, dateFrom);
        return List.of();
    }

    @Override
    public List<RailTransaction> listTransactions(String accountId, LocalDate dateFrom, LocalDate dateTo) {
        log.debug("Listing transactions [accountId: {}, from: {}, to: {}]", accountId, dateFrom, dateTo);
        return List.of();
    }
}
