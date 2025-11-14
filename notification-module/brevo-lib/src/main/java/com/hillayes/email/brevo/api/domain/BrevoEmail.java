package com.hillayes.email.brevo.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BrevoEmail {
    @JsonProperty("sender")
    private BrevoSender sender;

    @JsonProperty("to")
    private List<BrevoRecipient> to;

    @JsonProperty("bcc")
    private List<BrevoRecipient> bcc;

    @JsonProperty("cc")
    private List<BrevoRecipient> cc;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("htmlContent")
    private String htmlContent;
}
