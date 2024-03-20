package com.hillayes.rail.event;

import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.consent.*;
import com.hillayes.events.sender.EventSender;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.service.InstitutionService;
import com.hillayes.rail.utils.TestApiData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ConsentEventSenderTest {
    @InjectMock
    EventSender eventSender;

    @InjectMock
    InstitutionService institutionService;

    @Inject
    ConsentEventSender fixture;

    @Test
    public void testSendConsentInitiated() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now())
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri("http://mock/callback")
            .status(ConsentStatus.INITIATED)
            .build();

        // when: the fixture is called
        fixture.sendConsentInitiated(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentInitiated> captor = ArgumentCaptor.forClass(ConsentInitiated.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentInitiated event = captor.getValue();
        assertNotNull(event.getDateInitiated());
            assertEquals(userConsent.getId() , event.getConsentId());
            assertEquals(userConsent.getUserId(), event.getUserId());
            assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
            assertEquals(institution.getName(), event.getInstitutionName());
            assertEquals(userConsent.getAgreementId(), event.getAgreementId());
            assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
    }

    @Test
    public void testSendConsentGiven() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now())
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri(null)
            .status(ConsentStatus.GIVEN)
            .dateGiven(Instant.now())
            .build();

        // when: the fixture is called
        fixture.sendConsentGiven(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentGiven> captor = ArgumentCaptor.forClass(ConsentGiven.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentGiven event = captor.getValue();
        assertEquals(userConsent.getId() , event.getConsentId());
        assertEquals(userConsent.getUserId(), event.getUserId());
        assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
        assertEquals(institution.getName() , event.getInstitutionName());
        assertEquals(userConsent.getAgreementId(), event.getAgreementId());
        assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
        assertEquals(userConsent.getDateGiven(), event.getDateGiven());
    }

    @Test
    public void testSendConsentDenied() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofDays(10)))
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri(null)
            .status(ConsentStatus.DENIED)
            .dateGiven(Instant.now().minus(Duration.ofDays(10)))
            .dateDenied(Instant.now())
            .build();

        // when: the fixture is called
        fixture.sendConsentDenied(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentDenied> captor = ArgumentCaptor.forClass(ConsentDenied.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentDenied event = captor.getValue();
        assertEquals(userConsent.getId() , event.getConsentId());
        assertEquals(userConsent.getUserId(), event.getUserId());
        assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
        assertEquals(institution.getName() , event.getInstitutionName());
        assertEquals(userConsent.getAgreementId(), event.getAgreementId());
        assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
        assertEquals(userConsent.getDateDenied(), event.getDateDenied());
    }

    @Test
    public void testSendConsentCancelled() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofDays(10)))
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri(null)
            .status(ConsentStatus.CANCELLED)
            .dateGiven(Instant.now().minus(Duration.ofDays(10)))
            .dateCancelled(Instant.now())
            .build();

        // when: the fixture is called
        fixture.sendConsentCancelled(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentCancelled> captor = ArgumentCaptor.forClass(ConsentCancelled.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentCancelled event = captor.getValue();
        assertEquals(userConsent.getId() , event.getConsentId());
        assertEquals(userConsent.getUserId(), event.getUserId());
        assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
        assertEquals(institution.getName() , event.getInstitutionName());
        assertEquals(userConsent.getAgreementId(), event.getAgreementId());
        assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
        assertEquals(userConsent.getDateCancelled(), event.getDateCancelled());
    }

    @Test
    public void testSendConsentSuspended() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofDays(10)))
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri(null)
            .status(ConsentStatus.SUSPENDED)
            .dateGiven(Instant.now().minus(Duration.ofDays(10)))
            .dateCancelled(Instant.now())
            .build();

        // when: the fixture is called
        fixture.sendConsentSuspended(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentSuspended> captor = ArgumentCaptor.forClass(ConsentSuspended.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentSuspended event = captor.getValue();
        assertEquals(userConsent.getId() , event.getConsentId());
        assertEquals(userConsent.getUserId(), event.getUserId());
        assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
        assertEquals(institution.getName() , event.getInstitutionName());
        assertEquals(userConsent.getAgreementId(), event.getAgreementId());
        assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
        assertNotNull(event.getDateSuspended());
    }

    @Test
    public void testSendConsentExpired() {
        // given: a known institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(any(), eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a consent record
        UserConsent userConsent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .dateCreated(Instant.now().minus(Duration.ofDays(10)))
            .institutionId(institution.getId())
            .agreementId(UUID.randomUUID().toString())
            .maxHistory(80)
            .agreementExpires(Instant.now().plus(Duration.ofDays(80)))
            .callbackUri(null)
            .status(ConsentStatus.EXPIRED)
            .dateGiven(Instant.now().minus(Duration.ofDays(10)))
            .dateCancelled(Instant.now())
            .build();

        // when: the fixture is called
        fixture.sendConsentExpired(userConsent);

        // then: the correct event is emitted
        ArgumentCaptor<ConsentExpired> captor = ArgumentCaptor.forClass(ConsentExpired.class);
        verify(eventSender).send(eq(Topic.CONSENT), captor.capture());

        // and: the content is correct
        ConsentExpired event = captor.getValue();
        assertEquals(userConsent.getId() , event.getConsentId());
        assertEquals(userConsent.getUserId(), event.getUserId());
        assertEquals(userConsent.getInstitutionId() , event.getInstitutionId());
        assertEquals(institution.getName() , event.getInstitutionName());
        assertEquals(userConsent.getAgreementId(), event.getAgreementId());
        assertEquals(userConsent.getAgreementExpires(), event.getAgreementExpires());
        assertNotNull(event.getDateExpired());
    }
}
