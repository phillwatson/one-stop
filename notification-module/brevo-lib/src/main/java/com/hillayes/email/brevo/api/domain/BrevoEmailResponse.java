package com.hillayes.email.brevo.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BrevoEmailResponse {
    /**
     * Message ID of the transactional email scheduled.
     */
    @JsonProperty("messageId")
    private String messageId;

    /**
     * Array of message ID of the transactional emails scheduled
     */
    @JsonProperty("messageIds")
    private String messageIds;

    /**
     * Batch ID of the batch transactional email scheduled.
     */
    @JsonProperty("batchId")
    private String batchId;
}
