package com.hillayes.integration.test;

import org.junit.ClassRule;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class TestBase {
    @ClassRule
    public static DockerComposeContainer<?> environment =
        new DockerComposeContainer<>(
            new File("docker-compose.yml"),
            new File("docker-compose.override.yml")
        );
}
