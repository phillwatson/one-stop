package com.hillayes.events.events.user;

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
public class UserCreated {
    private UUID userId;

    private String username;

    private String email;

    private String title;

    private String givenName;

    private String familyName;

    private String preferredName;

    private String phoneNumber;

    private Instant dateCreated;
}
