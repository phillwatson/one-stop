package com.hillayes.email.api.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder(builderClassName = "Builder")
@ToString(onlyExplicitlyIncluded = true)
@Getter
public class EmailSender {
    private String name;

    @ToString.Include
    private String email;
}
