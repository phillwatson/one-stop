package com.hillayes.events.events.auth;

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
public class AccountActivity {
    private UUID userId;
    private SuspiciousActivity activity;
    private Instant dateRecorded;
}
