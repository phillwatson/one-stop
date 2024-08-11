package com.hillayes.rail.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.audit.AuditIssuesFound;
import com.hillayes.outbox.sender.EventSender;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AuditEventSender {
    private final EventSender eventSender;

    public void sendAuditIssuesFound(UUID userId, Map<String, Integer> issueCounts) {
        log.debug("Sending AuditIssuesFound event [userId: {}, issues: {}]", userId, issueCounts.size());

        eventSender.send(Topic.TRANSACTION_AUDIT, AuditIssuesFound.builder()
            .userId(userId)
            .dateDetected(Instant.now())
            .issueCounts(issueCounts)
            .build());
    }
}
