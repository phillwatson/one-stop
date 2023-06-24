package com.hillayes.events.events.auth;

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
public class UserLogin {
    private UUID userId;

    private Instant dateLogin;

    // the HTTP request header "User-Agent" from the login request
    private String userAgent;
}
