package com.hillayes.events.events.auth;

import com.hillayes.events.events.UserLocation;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class UserAuthenticated {
    private UUID userId;

    private Instant dateLogin;

    // the OiDC auth-provider that authenticated the user, null for username login
    private String authProvider;

    // the HTTP request header "User-Agent" from the login request
    private String userAgent;

    // the user's location, extracted from the HTTP request headers
    private UserLocation userLocation;
}
