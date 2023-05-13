package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to delete the stubs for a Requisition when that requisition
 * is deleted.
 */
@Slf4j
class DeleteRequisitions extends DeleteStubsExtension {
    DeleteRequisitions(NordigenSimulator simulator) {
        super(simulator);
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        // delete the stubs for the requisition identified by the parameter
        String requisitionId = parameters.get("id").toString();
        log.trace("Deleting stubs for requisition {}", requisitionId);

        simulator.popRequisition(requisitionId)
            .ifPresent(stubbings -> {
                stubbings.getStubs().forEach(admin::removeStubMapping);

                // delete the stubs for the agreement associated with the requisition
                log.trace("Deleting stubs for agreement {}", stubbings.getEntity().agreement);
                simulator.popAgreement(stubbings.getEntity().agreement)
                    .map(EntityStubs::getStubs)
                    .ifPresent(stubs -> stubs.forEach(admin::removeStubMapping));
            });
    }
}
