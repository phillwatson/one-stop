package com.hillayes.events.events.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Issued when an onboarding user acknowledges the magic token sent in
 * an email.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAcknowledged {
    private UUID userId;

    private String email;
}
