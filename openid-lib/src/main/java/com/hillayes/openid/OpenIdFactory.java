package com.hillayes.openid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.openid.rest.OpenIdConfigResponse;
import com.hillayes.openid.rest.OpenIdTokenApi;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.NoSuchElementException;

/**
 * A collection of Producer/Factory methods to inject and configure the OpenID
 * auth provider implementations. Each configuration can be injected using the
 * Qualifier @NamedAuthProvider. For example; to inject a IdTokenValidator for
 * validating Google ID-Tokens:
 *
 * <pre>
 *   \@Inject
 *   \@NamedAuthProvider(AuthProvider.GOOGLE)
 *   IdTokenValidator idTokenValidator;
 * </pre>
 */
@Slf4j
public abstract class OpenIdFactory {
    /**
     * An ObjectMapper instance. Is configured to ignore unknown properties (by disabling
     * the DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES). Also configured to format
     * dates and time in ISO-8601 (by disabling the SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).
     */
    @Inject
    ObjectMapper mapper;

    /**
     * The LOCAL configuration (i.e. from application.yaml) containing configuration properties
     * for all OpenId providers supported by the application.
     * <p>
     * From this the auth provider's own configuration can be retrieved.
     *
     * @see #openApiConfig(OpenIdConfiguration.AuthConfig)
     */
    @Inject
    public OpenIdConfiguration openIdConfiguration;

    protected VerificationKeyResolver verificationKeys(OpenIdConfigResponse openIdConfig) {
        log.debug("Using key-set [url: {}]", openIdConfig.jwksUri);
        return new HttpsJwksVerificationKeyResolver(new HttpsJwks(openIdConfig.jwksUri));
    }

    protected IdTokenValidator idTokenValidator(VerificationKeyResolver verificationKeys,
                                                OpenIdConfiguration.AuthConfig config,
                                                OpenIdConfigResponse openIdConfig) {
        log.debug("Creating token validator [issuer: {}]", openIdConfig.issuer);
        return new IdTokenValidator(verificationKeys, openIdConfig.issuer, config.clientId());
    }

    protected OpenIdConfigResponse openApiConfig(OpenIdConfiguration.AuthConfig config) throws IOException {
        log.debug("Retrieving OpenId Config [url: {}]", config.configUri());

        URL url = new URL(config.configUri());
        return mapper.readValue(url, OpenIdConfigResponse.class);
    }

    protected OpenIdTokenApi openIdRestApi(OpenIdConfiguration.AuthConfig authConfig,
                                           OpenIdConfigResponse openIdConfig) {
        String tokenEndpoint = openIdConfig.tokenEndpoint == null
            ? authConfig.tokenEndpoint().orElseThrow(() -> new NoSuchElementException("Missing tokenEndpoint"))
            : openIdConfig.tokenEndpoint;
        log.debug("Creating OpenId REST API [url: {}]", tokenEndpoint);

        URI baseUri = URI.create(tokenEndpoint);
        return RestClientBuilder.newBuilder()
            .baseUri(baseUri)
            .build(OpenIdTokenApi.class);
    }
}
