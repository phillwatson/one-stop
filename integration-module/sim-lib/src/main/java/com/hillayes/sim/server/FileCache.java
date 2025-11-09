package com.hillayes.sim.server;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple cache for files (located under the resources folder).
 */
public class FileCache {
    private static final Map<String,String> cache = new HashMap<>();

    public static final String loadFile(String filename) {
        return cache.computeIfAbsent(filename, key -> {
            try {
                URL resource = FileCache.class.getResource(key);
                return IOUtils.toString(resource, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }
}
