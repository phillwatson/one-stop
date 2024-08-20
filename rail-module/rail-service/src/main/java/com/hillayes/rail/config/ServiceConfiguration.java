package com.hillayes.rail.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "one-stop.rail")
public interface ServiceConfiguration {
    /**
     * The durations of various caches used to avoid unnecessary calls to the rail service.
     */
    Caches caches();

    /**
     * The list of countries that are supported by the rail service.
     */
    List<Country> countries();

    /**
     * The interval at which account details are polled. If a request is made to poll
     * the account details before this interval has elapsed, it will be ignored.
     */
    @WithDefault("PT1H")
    Duration accountPollingInterval();

    /**
     * The duration after which an INITIATED consent registration is considered to have
     * timed out, if no confirmation of its acceptance or denial has been received
     * from the rail.
     */
    @WithDefault("PT10M")
    Duration consentTimeout();

    Categories categories();

    Audit audit();

    /**
     * The detail that describes a country supported by the rail service.
     */
    interface Country {
        String id();
        String name();
        Optional<String> flagUri();
    }

    interface Caches {
        /**
         * The duration for which the list of institutions is cached.
         */
        Duration institutions();

        /**
         * The duration for which the account details are cached.
         */
        Duration accountDetails();
    }

    interface Categories {
        /**
         * The name of the category that is used for transactions that do not match any
         * category selector.
         */
        @WithDefault("Uncategorised")
        String uncategorisedName();

        /**
         * The default colour for a category.
         */
        @WithDefault("#dee0da")
        String defaultColour();
    }

    interface Audit {
        Issues issues();
    }

    interface Issues {
        Optional<Duration> ackTimeout();
    }
}
