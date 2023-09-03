package com.hillayes.integration.test;

import com.hillayes.sim.email.SendWithBlueSimulator;
import com.hillayes.sim.nordigen.NordigenSimClient;
import com.hillayes.sim.nordigen.NordigenSimulator;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiTestBase {
    /**
     * The URI on which the nordigen simulator is running. This is INTERNAL to docker network.
     * This is passed to the Rail Service container (via the config properties) to allow it
     * to connect to the simulator.
     */
    public static final String RAIL_HOST = "http://sim:8080" + NordigenSimulator.BASE_URI;

    /**
     * The URI on which the email simulator is running. This is INTERNAL to docker work.
     * This is passed to the Email Service container (via the config properties) to allow it
     * to connect to the simulator.
     */
    public static final String EMAIL_HOST = "http://wiremock:8080" + SendWithBlueSimulator.BASE_URI;

    private static final AtomicBoolean initialized = new AtomicBoolean();

    private static DockerComposeContainer<?> dockerContainers;

    @BeforeAll
    public static void beforeAll() {
        if (! initialized.getAndSet(true)) {
            dockerContainers = initContainers();
            dockerContainers.start();
            RestAssured.port = dockerContainers.getServicePort("client_1", 80);
        }
    }

    /**
     * Returns the port number on which the Wiremock server is listening. This can
     * be passed to a Wiremock client constructor, to allow it to connect.
     */
    public int getWiremockPort() {
        return dockerContainers.getServicePort("wiremock_1", 8080);
    }

    private static DockerComposeContainer<?> initContainers() {
        return new DockerComposeContainer<>(
            new File("../../one-stop/docker-compose.yaml"),
            resourceFile("/docker-compose.test.yaml"))
            .withExposedService("client_1", 80)
            .withExposedService("wiremock_1", 8080)
            .withEnv("ONE_STOP_EMAIL_SERVICE_URL", EMAIL_HOST)
            .withEnv("REST_CLIENT_NORDIGEN_API_URL", RAIL_HOST)
            .waitingFor("client_1", new HttpWaitStrategy().forPort(80).forPath("/api/v1/auth/jwks.json"));
    }

    /**
     * Creates a new client connected to the Rail Simulator which is running in a docker
     * container. The instance can be reused by calling its reset() method.
     */
    protected static NordigenSimClient newRailClient() {
        return NordigenSimulator.client("http://localhost:9090");
    }

    private static File resourceFile(String filename) {
        URL resource = ApiTestBase.class.getResource(filename);
        if (resource == null) {
            throw new RuntimeException("Unable to locate " + filename + ".\nIt should be in the test resources folder.");
        }

        return new File(resource.getFile());
    }
}
