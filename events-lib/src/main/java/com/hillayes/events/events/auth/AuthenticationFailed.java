package com.hillayes.events.events.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class AuthenticationFailed {
    private String username;

    private Instant dateLogin;

    private String reason;

    // the HTTP request header "User-Agent" from the login request
    private String userAgent;
}
