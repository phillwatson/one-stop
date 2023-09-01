package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.nordigen.model.Requisition;
import com.hillayes.nordigen.model.RequisitionStatus;
import com.hillayes.rail.service.RequisitionService;
import com.hillayes.rail.service.UserConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PollConsentJobbingTaskTest {
    private UserConsentService userConsentService;
    private RequisitionService requisitionService;
    private PollAccountJobbingTask pollAccountJobbingTask;
    private SchedulerFactory scheduler;

    private PollConsentJobbingTask fixture;

    @BeforeEach
    public void init() {
        userConsentService = mock();
        requisitionService = mock();
        pollAccountJobbingTask = mock();
        scheduler = mock();

        fixture = new PollConsentJobbingTask(
            userConsentService,
            requisitionService,
            pollAccountJobbingTask);
    }

    @Test
    public void testGetName() {
        assertEquals("poll-consent", fixture.getName());
    }

    @Test
    public void testQueueJob() {
        // given: the jobbing task has been configured
        fixture.taskInitialised(scheduler);

        // when: a user-consent ID is queued for processing
        UUID consentId = UUID.randomUUID();
        fixture.queueJob(consentId);

        // then: the job is passed to the scheduler for queuing
        verify(scheduler).addJob(fixture, consentId);
    }

    @Test
    public void testHappyPath() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a "linked" requisition for that consent
        Requisition requisition = Requisition.builder()
            .id(userConsent.getRequisitionId())
            .status(RequisitionStatus.LN)
            .accounts(List.of(
                randomAlphanumeric(20),
                randomAlphanumeric(20)
            ))
            .build();
        when(requisitionService.get(requisition.id)).thenReturn(Optional.of(requisition));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(requisitionService).get(requisition.id);

        // and: a job is queued to process each rail account in the requisition
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(pollAccountJobbingTask, times(requisition.accounts.size())).queueJob(eq(userConsent.getId()), captor.capture());

        // and: the rail-account IDs are correct
        requisition.accounts.forEach( railAccountId ->
            assertTrue(captor.getAllValues().contains(railAccountId))
        );

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testUserConsentNotFound() {
        // given: a missing consent record to be processed
        UUID userConsentId = UUID.randomUUID();
        when(userConsentService.getUserConsent(userConsentId)).thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsentId);
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsentId);

        // and: NO requisition is retrieved
        verifyNoInteractions(requisitionService);

        // and: NO job is queued to process each rail accounts
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode= EnumSource.Mode.EXCLUDE, names = { "GIVEN" })
    public void testUserConsentIsNotGiven(ConsentStatus consentStatus) {
        // given: a consent record to be processed - of non-GIVEN status
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(consentStatus)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: NO requisition is retrieved
        verifyNoInteractions(requisitionService);

        // and: NO job is queued to process each rail accounts
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testRequisitionExpired() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: an "expired" requisition for that consent
        Requisition requisition = Requisition.builder()
            .id(userConsent.getRequisitionId())
            .status(RequisitionStatus.EX)
            .accounts(List.of(
                randomAlphanumeric(20),
                randomAlphanumeric(20)
            ))
            .build();
        when(requisitionService.get(requisition.id)).thenReturn(Optional.of(requisition));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(requisitionService).get(requisition.id);

        // and: NO job is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the consent service is called to process expired requisition
        verify(userConsentService).consentExpired(userConsent.getId());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testRequisitionSuspended() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a "suspended" requisition for that consent
        Requisition requisition = Requisition.builder()
            .id(userConsent.getRequisitionId())
            .status(RequisitionStatus.SU)
            .accounts(List.of(
                randomAlphanumeric(20),
                randomAlphanumeric(20)
            ))
            .build();
        when(requisitionService.get(requisition.id)).thenReturn(Optional.of(requisition));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(requisitionService).get(requisition.id);

        // and: NO job is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the consent service is called to process suspended requisition
        verify(userConsentService).consentSuspended(userConsent.getId());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "LN", "EX", "SU" })
    public void testRequisitionStatusNotCorrect(RequisitionStatus requisitionStatus) {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a requisition for that consent
        Requisition requisition = Requisition.builder()
            .id(userConsent.getRequisitionId())
            .status(requisitionStatus)
            .accounts(List.of(
                randomAlphanumeric(20),
                randomAlphanumeric(20)
            ))
            .build();
        when(requisitionService.get(requisition.id)).thenReturn(Optional.of(requisition));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(requisitionService).get(requisition.id);

        // and: NO job is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the consent service is NOT called to process requisition
        verifyNoMoreInteractions(userConsentService);

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testRequisitionMissing() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .requisitionId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a requisition for that consent cannot be found
        when(requisitionService.get(userConsent.getRequisitionId())).thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: an attempt to retrieve the requisition is made
        verify(requisitionService).get(userConsent.getRequisitionId());

        // and: NO job is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountJobbingTask);

        // and: the consent service is NOT called to process requisition
        verifyNoMoreInteractions(userConsentService);

        // and: the task's result is INCOMPLETE
        assertEquals(TaskConclusion.INCOMPLETE, result);
    }
}
