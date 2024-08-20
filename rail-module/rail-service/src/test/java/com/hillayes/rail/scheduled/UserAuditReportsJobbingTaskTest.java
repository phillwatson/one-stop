package com.hillayes.rail.scheduled;

import com.hillayes.commons.jpa.Page;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.rail.audit.AuditReportTemplate;
import com.hillayes.rail.domain.AuditIssue;
import com.hillayes.rail.domain.AuditReportConfig;
import com.hillayes.rail.event.AuditEventSender;
import com.hillayes.rail.repository.AuditIssueRepository;
import com.hillayes.rail.repository.AuditReportConfigRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.hillayes.rail.utils.TestData.mockAuditReportConfig;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserAuditReportsJobbingTaskTest {
    @Inject
    Instance<AuditReportTemplate> reportTemplates;

    @ConfigProperty(name = "one-stop.rail.audit.issues.ack-timeout")
    Optional<Duration> ackTimeout;

    @InjectMock
    AuditReportConfigRepository auditReportConfigRepository;

    @InjectMock
    AuditIssueRepository auditIssueRepository;

    @InjectMock
    AuditEventSender auditEventSender;

    @Inject
    UserAuditReportsJobbingTask fixture;

    @BeforeEach
    public void init() {
        when(auditIssueRepository.save(any())).thenAnswer(i -> {
            AuditIssue issue = i.getArgument(0);
            if (issue.getId() == null) {
                issue.setId(UUID.randomUUID());
            }
            return issue;
        });
    }

    @Test
    public void testApply() {
        // given: a user ID
        UUID userId = UUID.randomUUID();

        // and: a payload identifying the user
        TaskContext<UserAuditReportsJobbingTask.Payload> context =
            new TaskContext<>(new UserAuditReportsJobbingTask.Payload(userId));

        // and: a collection of report templates exist
        List<AuditReportTemplate> templateList = reportTemplates.stream().toList();
        assertFalse(templateList.isEmpty());

        // and: a collection of the user's report configurations
        List<AuditReportConfig> reportConfigs = IntStream.range(1, 10)
            .mapToObj(i -> templateList.get(nextInt(0, templateList.size())))
            .map(template -> mockAuditReportConfig(userId, c -> c
                .id(UUID.randomUUID())
                .templateName(template.getName())
            ))
            .toList();
        when(auditReportConfigRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .then(invocation -> {
                int pageIndex = invocation.getArgument(1);
                int pageSize = invocation.getArgument(2);
                return Page.of(reportConfigs, pageIndex, pageSize);
            });

        // and: an acknowledged timeout is configured
        Duration expectedTimeout = ackTimeout.orElse(null);
        assertNotNull(expectedTimeout);

        // when: the job is invoked
        fixture.apply(context);

        // then: for each of the users report configurations
        ArgumentCaptor<Duration> timeoutCapture = ArgumentCaptor.forClass(Duration.class);
        reportConfigs.forEach(config -> {
            // and: the old acknowledged issues are delete for each report config
            verify(auditIssueRepository).deleteAcknowledged(eq(config.getId()), timeoutCapture.capture());
            assertEquals(expectedTimeout, timeoutCapture.getValue());
        });
    }
}
