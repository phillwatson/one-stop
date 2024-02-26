package com.hillayes.commons.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public class Network {
    /**
     * Returns the external IP of the calling service host.
     *
     * @return the calling service's external IP address.
     * @throws IOException if the IP address cannot be obtained.
     */
    public static String getMyIpAddress() throws IOException {
        URL url = URI.create("http://checkip.amazonaws.com/").toURL();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.readLine();
        }
    }
}
