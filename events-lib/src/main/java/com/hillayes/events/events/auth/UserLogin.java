package com.hillayes.events.events.auth;

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
public class UserLogin {
    private UUID userId;

    private Instant dateLogin;

    // the HTTP request header "User-Agent" from the login request
    private String userAgent;
}
