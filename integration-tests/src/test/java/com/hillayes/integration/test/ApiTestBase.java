package com.hillayes.integration.test;

import com.hillayes.integration.sim.email.SendWithBlueSimulator;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiTestBase {
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
            .withEnv("ONE_STOP_EMAIL_SERVICE_URL", "http://wiremock:8080" + SendWithBlueSimulator.BASE_URI)
            .withEnv("NORDIGEN_API_URL", "http://wiremock:8080") // + NordigenSimulator.BASE_URI)
            .waitingFor("client_1", new HttpWaitStrategy().forPort(80).forPath("/api/v1/auth/jwks.json"));
    }

    private static File resourceFile(String filename) {
        URL resource = ApiTestBase.class.getResource(filename);
        if (resource == null) {
            throw new RuntimeException("Unable to locate " + filename + ".\nIt should be in the test resources folder.");
        }

        return new File(resource.getFile());
    }
}
