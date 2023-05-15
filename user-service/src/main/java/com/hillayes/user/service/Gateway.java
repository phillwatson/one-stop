package com.hillayes.user.service;

import com.hillayes.commons.net.Network;
import com.hillayes.user.config.ServiceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class Gateway {
    private final ServiceConfig serviceConfig;

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
