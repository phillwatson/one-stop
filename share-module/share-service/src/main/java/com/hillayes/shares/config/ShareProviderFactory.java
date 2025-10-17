package com.hillayes.shares.config;

import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.errors.ProviderNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.hillayes.shares.api.ShareProviderApi;

import java.util.stream.Stream;

/**
 * A common factory for accessing ShareProviderApi implementations.
 */
@ApplicationScoped
public class ShareProviderFactory {
    private Instance<ShareProviderApi> shareProviderApis;

    public ShareProviderFactory(@Any Instance<ShareProviderApi> allProviders) {
        shareProviderApis = allProviders;
    }

    /**
     * Returns the ShareProviderApi implementation for the ShareProvider identified
     * by the given String.
     * @param providerId the string value of the ShareProvider enum.
     * @return the identified ShareProviderApi implementation.
     */
    public ShareProviderApi getImplementation(String providerId) {
        try {
            ShareProvider provider = ShareProvider.valueOf(providerId);
            return get(provider);
        } catch (IllegalArgumentException e) {
            throw new ProviderNotFoundException(providerId);
        }
    }

    /**
     * Returns the ShareProviderApi implementation for the given ShareProvider. Will
     * throw an IllegalArgumentException if no implementation is found.
     * @param provider the ShareProvider value.
     * @return the identified ShareProviderApi implementation.
     */
    public ShareProviderApi get(ShareProvider provider) {
        return shareProviderApis.stream()
            .filter(api -> api.isFor(provider))
            .findFirst()
            .orElseThrow(() -> new ProviderNotFoundException(provider));
    }

    /**
     * Returns all available ShareProviderApi implementations.
     */
    public Stream<ShareProviderApi> getAll() {
        return shareProviderApis.stream();
    }
}
