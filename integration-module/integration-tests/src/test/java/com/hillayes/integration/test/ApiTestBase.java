package com.hillayes.integration.test;

import com.hillayes.onestop.api.ServiceErrorResponse;
import com.hillayes.sim.email.SendWithBlueSimulator;
import com.hillayes.sim.nordigen.NordigenSimClient;
import com.hillayes.sim.nordigen.NordigenSimulator;
import com.hillayes.sim.yapily.YapilySimClient;
import com.hillayes.sim.yapily.YapilySimulator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ApiTestBase {
    /**
     * The port exposed by the wiremock container.
     */
    private static final int WIREMOCK_PORT = 8080;

    /**
     * The port exposed by the http client - to which all service traffic is directed
     */
    private static final int CLIENT_PORT = 80;

    /**
     * The port exposed by the nordigen rail simulator.
     */
    private static final int RAIL_PORT = 9090;

    /**
     * The URI on which the nordigen simulator is running. This is INTERNAL to docker network.
     * This is passed to the Rail Service container (via the config properties) to allow it
     * to connect to the simulator.
     */
    public static final String NORDIGEN_RAIL_HOST = "http://sim:" + WIREMOCK_PORT + NordigenSimulator.BASE_URI;

    /**
     * The URI on which the yapily simulator is running. This is INTERNAL to docker network.
     * This is passed to the Rail Service container (via the config properties) to allow it
     * to connect to the simulator.
     */
    public static final String YAPILY_RAIL_HOST = "http://sim:" + WIREMOCK_PORT + YapilySimulator.BASE_URI;

    /**
     * The URI on which the email simulator is running. This is INTERNAL to docker work.
     * This is passed to the Email Service container (via the config properties) to allow it
     * to connect to the simulator.
     */
    public static final String EMAIL_HOST = "http://wiremock:" + WIREMOCK_PORT + SendWithBlueSimulator.BASE_URI;

    // a semaphore to ensure that the docker containers are only initialized once
    private static final AtomicBoolean initialized = new AtomicBoolean();

    private static DockerComposeContainer<?> dockerContainers;

    @BeforeAll
    public static void beforeAll() {
        if (! initialized.getAndSet(true)) {
            dockerContainers = initContainers();
            dockerContainers.start();

            // all calls go through the client container (nginx gateway)
            RestAssured.port = dockerContainers.getServicePort("client_1", CLIENT_PORT);
        }
    }

    /**
     * Returns the port number on which the Wiremock server is listening. This can
     * be passed to a Wiremock client constructor, to allow it to connect.
     */
    public int getWiremockPort() {
        return dockerContainers.getServicePort("wiremock_1", WIREMOCK_PORT);
    }

    private static DockerComposeContainer<?> initContainers() {
        return new DockerComposeContainer<>(
            new File("../../docker-compose.yaml"),
            resourceFile("/docker-compose.test.yaml"))
            .withExposedService("client_1", CLIENT_PORT)
            .withExposedService("wiremock_1", WIREMOCK_PORT)
            .withEnv("ONE_STOP_EMAIL_SERVICE_URL", EMAIL_HOST)
            .withEnv("REST_CLIENT_NORDIGEN_API_URL", NORDIGEN_RAIL_HOST)
            .withEnv("REST_CLIENT_YAPILY_API_URL", YAPILY_RAIL_HOST)
            .withBuild(true) // force build of client image
            .waitingFor("client_1", new HttpWaitStrategy().forPort(CLIENT_PORT).forPath("/api/v1/auth/jwks.json"));
    }

    /**
     * Creates a new client connected to the Rail Simulator which is running in a docker
     * container. The instance can be reused by calling its reset() method.
     */
    protected static NordigenSimClient newNordigenClient() {
        return NordigenSimulator.client("http://localhost:" + RAIL_PORT);
    }

    /**
     * Creates a new client connected to the Rail Simulator which is running in a docker
     * container. The instance can be reused by calling its reset() method.
     */
    protected static YapilySimClient newYapilyClient() {
        return YapilySimulator.client("http://localhost:" + RAIL_PORT);
    }

    private static File resourceFile(String filename) {
        URL resource = ApiTestBase.class.getResource(filename);
        if (resource == null) {
            throw new RuntimeException("Unable to locate " + filename + ".\nIt should be in the test resources folder.");
        }

        return new File(resource.getFile());
    }

    protected void withServiceError(Response response, Consumer<ServiceErrorResponse> consumer) {
        consumer.accept(response.as(ServiceErrorResponse.class));
    }
}
