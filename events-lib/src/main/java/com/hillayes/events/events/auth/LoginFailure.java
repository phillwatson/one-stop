package com.hillayes.events.events.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginFailure {
    @NotNull
    private String username;

    @NotNull
    private Instant dateLogin;

    @NotNull
    private String reason;
}
