package com.hillayes.sim.server;

import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;

public interface SimExtension extends ResponseDefinitionTransformerV2 {
    default boolean applyGlobally() {
        return false;
    }
}
