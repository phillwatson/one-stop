package com.hillayes.events.events.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdated {
    @NotNull
    private UUID userId;

    @NotNull
    private String username;

    @NotNull
    private String email;

    private String givenName;

    private String familyName;

    @NotNull
    private Instant dateUpdated;
}
