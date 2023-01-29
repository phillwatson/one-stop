package com.hillayes.events.events.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmail {
    @NotNull
    private String toAddress;

    @NotNull
    private String fromAddress;

    @NotNull
    private String templateId;
}
