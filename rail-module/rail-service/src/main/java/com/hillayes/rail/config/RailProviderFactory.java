package com.hillayes.rail.config;

import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.RailProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.stream.Stream;

/**
 * A common factory for accessing RailProviderApi implementations.
 */
@ApplicationScoped
public class RailProviderFactory {
    @Inject
    @Any
    Instance<RailProviderApi> railProviderApis;

    /**
     * Returns the RailProviderApi implementation for the RailProvider identified
     * by the given String.
     * @param railProviderId the string value of the RailProvider enum.
     * @return the identified RailProviderApi implementation.
     */
    public RailProviderApi get(String railProviderId) {
        RailProvider railProvider = RailProvider.valueOf(railProviderId);
        return get(railProvider);
    }

    /**
     * Returns the RailProviderApi implementation for the given RailProvider. Will
     * throw an IllegalArgumentException if no implementation is found.
     * @param railProvider the RailProvider value.
     * @return the identified RailProviderApi implementation.
     */
    public RailProviderApi get(RailProvider railProvider) {
        return railProviderApis.stream()
            .filter(api -> api.isFor(railProvider))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No RailProviderApi found for " + railProvider));
    }

    /**
     * Returns all available RailProviderApi implementations.
     */
    public Stream<RailProviderApi> getAll() {
        return railProviderApis.stream();
    }
}
