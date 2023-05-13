package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.extension.PostServeAction;

/**
 * This class is used to delete the stubs that are no longer valid. For example;
 * the stubs to retrieve an entity will be invalid after the entity is deleted.
 */
public abstract class DeleteStubsExtension extends PostServeAction {
    protected final NordigenSimulator simulator;

    DeleteStubsExtension(NordigenSimulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
