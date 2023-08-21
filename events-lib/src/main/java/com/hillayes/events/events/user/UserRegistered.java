package com.hillayes.events.events.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

/**
 * Issued to initiate the onboarding process for a user. The user
 * provides an email, and a token is generated and sent to that
 * email address.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class UserRegistered {
    private String email;
    private Instant expires;
    private URI acknowledgerUri;
    private Locale locale;
}
