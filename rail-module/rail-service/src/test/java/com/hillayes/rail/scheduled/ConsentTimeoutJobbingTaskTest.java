package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.service.UserConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConsentTimeoutJobbingTaskTest {
    UserConsentService userConsentService = mock();

    ConsentTimeoutJobbingTask consentTimeoutJobbingTask;

    @BeforeEach
    public void setUp() {
        consentTimeoutJobbingTask = new ConsentTimeoutJobbingTask(userConsentService);
    }

    @Test
    public void testApply() {
        // given: a consent ID
        UUID consentId = UUID.randomUUID();

        // and: the consent ID is passed as the payload of a task context
        TaskContext<UUID> context = new TaskContext<>(consentId);

        // when: the apply method is called
        TaskConclusion result = consentTimeoutJobbingTask.apply(context);

        // then: the task is completed
        assertEquals(TaskConclusion.COMPLETE, result);

        // and: the consent service is called to mark the consent as timed out
        verify(userConsentService).registrationTimeout(consentId);
    }
}
