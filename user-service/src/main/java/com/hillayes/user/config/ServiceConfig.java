package com.hillayes.user.config;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration for the user service.
 */
@ConfigMapping(prefix = "one-stop")
public interface ServiceConfig {
    GatewayConfig gateway();

    /**
     * Holds properties for the gateway.
     */
    interface GatewayConfig {
        /**
         * The scheme to use for the gateway (http or https).
         */
        String scheme();

        /**
         * The port used by the gateway.
         */
        int openPort();
    }
}
