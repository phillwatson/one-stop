package com.hillayes.user.openid.google;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.JwtValidator;
import com.hillayes.user.openid.AuthProvider;
import com.hillayes.user.openid.NamedAuthProvider;
import com.hillayes.user.openid.OpenIdAuth;
import com.hillayes.user.openid.OpenIdConfiguration;
import com.hillayes.user.openid.rest.TokenExchangeRequest;
import com.hillayes.user.openid.rest.TokenExchangeResponse;
import com.hillayes.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import javax.inject.Inject;

@Slf4j
public class GoogleAuth extends OpenIdAuth {
    private final OpenIdConfiguration.AuthConfig config;

    private final JwtValidator jwtValidator;

    private final GoogleIdRestApi googleIdRestApi;

    public GoogleAuth(OpenIdConfiguration.AuthConfig config,
                      JwtValidator jwtValidator,
                      GoogleIdRestApi googleIdRestApi,
                      UserRepository userRepository,
                      PasswordCrypto passwordCrypto) {
        super(userRepository, passwordCrypto);
        this.config = config;
        this.jwtValidator = jwtValidator;
        this.googleIdRestApi = googleIdRestApi;
    }

    public JwtClaims exchangeAuthToken(String authCode) throws InvalidJwtException {
        TokenExchangeRequest request = TokenExchangeRequest.builder()
            .grantType("authorization_code")
            .redirectUri(config.redirectUri())
            .code(authCode)
            .clientId(config.clientId())
            .clientSecret(config.clientSecret().get())
            .build();

        TokenExchangeResponse response = googleIdRestApi.exchangeToken(request);
        log.trace("OAuth [idToken: {}, accessToken: {}]", response.idToken, response.accessToken);

        return jwtValidator.verify(response.idToken);
    }

    public String toString() {
        return "OpenIdAuth["+ config.configUri() + "]";
    }
}
