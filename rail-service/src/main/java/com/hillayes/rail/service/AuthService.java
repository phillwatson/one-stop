package com.hillayes.rail.service;

import com.hillayes.rail.model.ObtainJwtResponse;
import com.hillayes.rail.model.RefreshJwtResponse;
import com.hillayes.rail.repository.AuthRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AuthService {
    @Inject
    @RestClient
    AuthRepository authRepository;

    public ObtainJwtResponse newToken(String secretId,
                                      String secretKey) {
        return authRepository.newToken(secretId, secretKey);
    }

    public RefreshJwtResponse refreshToken(String refresh) {
        return authRepository.refreshToken(refresh);
    }
}
