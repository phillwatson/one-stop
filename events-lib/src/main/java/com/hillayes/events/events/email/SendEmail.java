package com.hillayes.events.events.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmail {
    @NotNull
    private UUID userId;

    @NotNull
    private String templateId;
}
