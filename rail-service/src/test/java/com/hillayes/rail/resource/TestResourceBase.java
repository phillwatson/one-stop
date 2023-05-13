package com.hillayes.rail.resource;

import com.hillayes.rail.simulator.NordigenSimulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class TestResourceBase extends TestBase {
    public final static NordigenSimulator nordigenSimulator = new NordigenSimulator(8089);

    @BeforeAll
    public static void beforeAll() {
        nordigenSimulator.start();
    }
    @AfterAll
    public static void afterAll() {
        nordigenSimulator.stop();
    }

    @BeforeEach
    public void beforeEach() {
        nordigenSimulator.reset();
    }

}
