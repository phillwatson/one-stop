package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.service.UserConsentService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConsentTimeoutAdhocTaskTest {
    UserConsentService userConsentService = mock();

    ConsentTimeoutAdhocTask consentTimeoutAdhocTask;

    @BeforeEach
    public void setUp() {
        consentTimeoutAdhocTask = new ConsentTimeoutAdhocTask(userConsentService);
    }

    @Test
    public void testApply() {
        // given: a consent ID
        UUID consentId = UUID.randomUUID();

        // and: the consent ID is passed as the payload of a task context
        ConsentTimeoutAdhocTask.Payload payload =
            new ConsentTimeoutAdhocTask.Payload(consentId, RandomStringUtils.insecure().nextAlphanumeric(30));
        TaskContext<ConsentTimeoutAdhocTask.Payload> context = new TaskContext<>(payload);

        // when: the apply method is called
        TaskConclusion result = consentTimeoutAdhocTask.apply(context);

        // then: the task is completed
        assertEquals(TaskConclusion.COMPLETE, result);

        // and: the consent service is called to mark the consent as timed out
        verify(userConsentService).registrationTimeout(payload.consentId(), payload.reference());
    }
}
