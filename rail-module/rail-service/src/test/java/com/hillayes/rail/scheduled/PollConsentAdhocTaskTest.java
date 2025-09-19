package com.hillayes.rail.scheduled;

import com.hillayes.executors.scheduler.SchedulerFactory;
import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.RailAgreement;
import com.hillayes.rail.api.domain.AgreementStatus;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.UserConsentService;
import com.hillayes.rail.utils.TestApiData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class PollConsentAdhocTaskTest {
    @Mock
    UserConsentService userConsentService;
    @Mock
    RailProviderFactory railProviderFactory;
    @Mock
    RailProviderApi railProviderApi;
    @Mock
    PollAccountAdhocTask pollAccountAdhocTask;
    @Mock
    SchedulerFactory scheduler;

    @InjectMocks
    PollConsentAdhocTask fixture;

    @BeforeEach
    public void init() {
        openMocks(this);

        when(railProviderFactory.get(any())).thenReturn(railProviderApi);
    }

    @Test
    public void testGetName() {
        assertEquals("poll-consent", fixture.getName());
    }

    @Test
    public void testQueueTask() {
        // given: the adhoc task has been configured
        fixture.taskInitialised(scheduler);

        // when: a user-consent ID is queued for processing
        UUID consentId = UUID.randomUUID();
        fixture.queueTask(consentId);

        // then: the task is passed to the scheduler for queuing
        verify(scheduler).addJob(fixture, consentId);
    }

    @Test
    public void testHappyPath() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a "linked" requisition for that consent
        RailAgreement agreement = TestApiData.mockAgreement(userConsent.getAgreementId());
        when(railProviderApi.getAgreement(agreement.getId())).thenReturn(Optional.of(agreement));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(railProviderApi).getAgreement(agreement.getId());

        // and: a task is queued to process each rail account in the requisition
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(pollAccountAdhocTask, times(agreement.getAccountIds().size()))
            .queueTask(eq(userConsent.getId()), captor.capture());

        // and: the rail-account IDs are correct
        agreement.getAccountIds().forEach(railAccountId ->
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
        verifyNoInteractions(railProviderApi);

        // and: NO task is queued to process each rail accounts
        verifyNoInteractions(pollAccountAdhocTask);

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
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: NO requisition is retrieved
        verifyNoInteractions(railProviderApi);

        // and: NO task is queued to process each rail accounts
        verifyNoInteractions(pollAccountAdhocTask);

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @Test
    public void testRequisitionExpired() {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: an "expired" requisition for that consent
        RailAgreement agreement = TestApiData.mockAgreement(userConsent.getAgreementId(), AgreementStatus.EXPIRED);
        when(railProviderApi.getAgreement(agreement.getId())).thenReturn(Optional.of(agreement));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(railProviderApi).getAgreement(agreement.getId());

        // and: NO task is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountAdhocTask);

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
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a "suspended" requisition for that consent
        RailAgreement agreement = TestApiData.mockAgreement(userConsent.getAgreementId(), AgreementStatus.SUSPENDED);
        when(railProviderApi.getAgreement(agreement.getId())).thenReturn(Optional.of(agreement));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(railProviderApi).getAgreement(agreement.getId());

        // and: NO task is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountAdhocTask);

        // and: the consent service is called to process suspended requisition
        verify(userConsentService).consentSuspended(userConsent.getId());

        // and: the task's result is COMPLETE
        assertEquals(TaskConclusion.COMPLETE, result);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "GIVEN", "EXPIRED", "SUSPENDED" })
    public void testRequisitionStatusNotCorrect(AgreementStatus agreementStatus) {
        // given: a consent record to be processed
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .status(ConsentStatus.GIVEN)
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a requisition for that consent
        RailAgreement agreement = TestApiData.mockAgreement(userConsent.getAgreementId(), agreementStatus);
        when(railProviderApi.getAgreement(agreement.getId())).thenReturn(Optional.of(agreement));

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: the requisition is retrieved
        verify(railProviderApi).getAgreement(agreement.getId());

        // and: NO task is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountAdhocTask);

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
            .agreementId(randomAlphanumeric(20))
            .build();
        when(userConsentService.getUserConsent(userConsent.getId())).thenReturn(Optional.of(userConsent));

        // and: a requisition for that consent cannot be found
        when(railProviderApi.getAgreement(userConsent.getAgreementId())).thenReturn(Optional.empty());

        // when: the fixture is called to process the user-consent
        TaskContext<UUID> context = new TaskContext<>(userConsent.getId());
        TaskConclusion result = fixture.apply(context);

        // then: the user-consent is retrieved
        verify(userConsentService).getUserConsent(userConsent.getId());

        // and: an attempt to retrieve the requisition is made
        verify(railProviderApi).getAgreement(userConsent.getAgreementId());

        // and: NO task is queued to process each rail account in the requisition
        verifyNoInteractions(pollAccountAdhocTask);

        // and: the consent service is NOT called to process requisition
        verifyNoMoreInteractions(userConsentService);

        // and: the task's result is INCOMPLETE
        assertEquals(TaskConclusion.INCOMPLETE, result);
    }
}
