package com.hillayes.email.brevo.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder(builderClassName = "Builder")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BrevoRecipient {
    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;
}
