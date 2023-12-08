package com.hillayes.events.events.auth;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Issued when suspicious activity is detected on a user account.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class AccountActivity {
    private UUID userId;
    private SuspiciousActivity activity;
    private Instant dateRecorded;

    // the HTTP request header "User-Agent" from the login request
    private String userAgent;
}
