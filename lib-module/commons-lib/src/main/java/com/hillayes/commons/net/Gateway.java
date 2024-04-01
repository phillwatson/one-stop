package com.hillayes.commons.net;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

/**
 * Provides the configuration for external access to the application.
 */
@ApplicationScoped
public class Gateway {
    @ConfigProperty(name = "one-stop.gateway.scheme")
    String scheme;

    @ConfigProperty(name = "one-stop.gateway.open-port")
    int openPort;

    /**
     * The http scheme on which the application is listening.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Tests whether the application is listening on a secure connection.
     */
    public boolean isSecure() {
        return "https".equalsIgnoreCase(getScheme());
    }

    /**
     * The port number on which the application is exposed to the outside world.
     * This port will need to be mapped to a forwarding port on the router.
     */
    public int getPort() {
        return openPort;
    }

    /**
     * The hostname, or IP address, on which the application is being hosted, and
     * is accessible to the outside world.
     */
    public String getHost() throws IOException {
        return Network.getMyIpAddress();
    }
}
