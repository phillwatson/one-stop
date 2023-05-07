package com.hillayes.events.events.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Issued to initiate the onboarding process for a user. The user
 * provides an email, and a token is generated and sent to that
 * email address.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistered {
    private String email;
    private String token;
    private Instant expires;
}
