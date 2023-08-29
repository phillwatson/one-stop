package com.hillayes.integration.test;

import io.restassured.RestAssured;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class ApiTestBase {
    private static boolean initialized = false;

    @ClassRule
    public static final DockerComposeContainer<?> environment;

    static {
        URL testDockerCompose = ApiTestBase.class.getResource("/docker-compose.test.yaml");
        if (testDockerCompose == null) {
            throw new RuntimeException("Unable to locate docker-compose.test.yaml.\nIt should be in the test resources folder.");
        }

        environment = new DockerComposeContainer<>(
            new File("/data/src/one-stop/docker-compose.yaml"),
            new File("/data/src/one-stop/docker-compose.override.yaml"),
            new File(testDockerCompose.getFile()))
            .withExposedService("client_1", 80)
            .withExposedService("wiremock_1", 8080)
            .withEnv("ONE_STOP_EMAIL_SERVICE_URL", "http://wiremock:8080/api.sendinblue.com/v3")
            .waitingFor("client_1", new HttpWaitStrategy().forPort(80).forPath("/api/v1/auth/jwks.json"));
    }

    @BeforeAll
    public static void beforeAll() {
        if (! initialized) {
            initialized = true;
            environment.start();
            RestAssured.port = environment.getServicePort("client_1", 80);
        }
    }
}
