package com.hillayes.rail.config;

import io.smallrye.config.ConfigMapping;

import java.util.List;

@ConfigMapping(prefix = "one-stop.rail")
public interface ServiceConfiguration {
    String callbackUrl();

    List<Country> countries();

    interface Country {
        String id();
        String name();
    }
}
