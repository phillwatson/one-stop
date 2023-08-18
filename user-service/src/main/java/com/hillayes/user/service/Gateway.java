package com.hillayes.user.service;

import com.hillayes.commons.net.Network;
import com.hillayes.user.config.ServiceConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class Gateway {
    private final ServiceConfiguration serviceConfig;

    public String getScheme() {
        return serviceConfig.gateway().scheme();
    }

    public int getPort() {
        return serviceConfig.gateway().openPort();
    }

    public String getHost() throws IOException {
        return Network.getMyIpAddress();
    }
}
