package com.hillayes.events.events.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdated {
    private UUID userId;

    private String username;

    private String email;

    private String title;

    private String givenName;

    private String familyName;

    private String preferredName;

    private String phoneNumber;

    // the user's preferred language.
    private Locale locale;

    private Instant dateUpdated;
}
