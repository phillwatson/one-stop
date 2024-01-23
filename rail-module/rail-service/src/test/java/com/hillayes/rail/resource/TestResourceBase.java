package com.hillayes.rail.resource;

import com.hillayes.rail.simulator.NordigenSimulator;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;

public class TestResourceBase extends TestBase {
    @Inject
    NordigenSimulator nordigenSimulator;

    @BeforeEach
    public void beforeEach() {
        nordigenSimulator.reset();
    }
}
