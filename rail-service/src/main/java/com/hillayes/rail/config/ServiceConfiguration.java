package com.hillayes.rail.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;
import java.util.List;

@ConfigMapping(prefix = "one-stop.rail")
public interface ServiceConfiguration {
    String callbackUrl();

    Caches caches();

    List<Country> countries();

    interface Country {
        String id();
        String name();
    }

    interface Caches {
        Duration institutions();
        Duration accountDetails();
    }
}
