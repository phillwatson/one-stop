package com.hillayes.rail.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
@RegisterForReflection
public class AuditIssueSummary {
    @EqualsAndHashCode.Include
    private UUID auditConfigId;

    private String auditConfigName;

    private long totalCount;

    private long acknowledgedCount;
}
