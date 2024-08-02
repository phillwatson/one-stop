package com.hillayes.integration.api;

import java.net.URI;
import java.util.UUID;

public class Utils {
    /**
     * Extracts the UUID from the last segment of the path of a URI.
     * @param location The URI to extract the UUID from.
     * @return The UUID extracted from the URI, or null if the URI is null or does not contain a UUID.
     */
    public static UUID getIdFromLocation(URI location) {
        if (location == null) {
            return null;
        }

        String path = location.getPath();
        int index = path.lastIndexOf('/') + 1;
        if ((index <= 0) || (index >= path.length())) {
            return null;
        }

        return UUID.fromString(path.substring(index));
    }
}
