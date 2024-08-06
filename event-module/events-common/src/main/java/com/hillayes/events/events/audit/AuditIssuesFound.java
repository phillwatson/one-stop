package com.hillayes.events.events.audit;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
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
     * The issues detected in the user's audit report.
     */
    private List<AuditIssue> issues;
}
