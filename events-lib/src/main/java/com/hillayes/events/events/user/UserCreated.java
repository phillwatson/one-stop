package com.hillayes.events.events.user;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreated {
    @NotNull
    private UUID userId;

    @NotNull
    private String username;

    @NotNull
    private String email;

    private String title;

    private String givenName;

    private String familyName;

    private String preferredName;

    private String phoneNumber;

    @NotNull
    private Instant dateCreated;
}
