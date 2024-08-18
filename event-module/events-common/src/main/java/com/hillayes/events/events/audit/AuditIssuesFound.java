package com.hillayes.events.events.audit;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class AuditIssuesFound {
    /**
     * The ID of the user from whom the audit issue was detected.
     */
    private UUID userId;

    /**
     * The date-time at which the issue was detected.
     */
    private Instant dateDetected;

    /**
     * A map of the names of the user's audit report configurations and the number
     * of issues found for each configuration.
     */
    private Map<String, Integer> issueCounts;
}
