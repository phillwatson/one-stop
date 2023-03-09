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
public class UserDeleted {
    @NotNull
    private UUID userId;

    @NotNull
    private Instant dateDeleted;
}
