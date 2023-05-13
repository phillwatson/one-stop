package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to delete the stubs for an EndUserAgreement when that agreement
 * is deleted.
 */
@Slf4j
class DeleteAgreements extends DeleteStubsExtension {
    DeleteAgreements(NordigenSimulator simulator) {
        super(simulator);
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        // delete the stubs for the agreement identified by the parameter
        String agreementId = parameters.get("id").toString();
        log.trace("Deleting stubs for agreement {}", agreementId);

        simulator.popAgreement(agreementId)
            .map(EntityStubs::getStubs)
            .ifPresent(stubs -> stubs.forEach(admin::removeStubMapping));
    }
}
