package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.net.Gateway;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.*;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.errors.BankRegistrationException;
import com.hillayes.rail.errors.DeleteRailConsentException;
import com.hillayes.rail.errors.RegistrationNotFoundException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.scheduled.ConsentTimeoutAdhocTask;
import com.hillayes.rail.scheduled.PollConsentAdhocTask;
import com.hillayes.rail.utils.TestApiData;
import com.hillayes.rail.utils.TestData;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserConsentServiceTest {
    @Mock
    UserConsentRepository userConsentRepository;

    @Mock
    InstitutionService institutionService;

    @Mock
    RailProviderFactory railProviderFactory;

    @Mock
    PollConsentAdhocTask pollConsentAdhocTask;

    @Mock
    ConsentTimeoutAdhocTask consentTimeoutAdhocTask;

    @Mock
    ConsentEventSender consentEventSender;

    @Mock
    ServiceConfiguration configuration;

    @Mock
    RailProviderApi railProviderApi;

    @Mock
    Gateway gateway;

    @InjectMocks
    UserConsentService fixture;

    @BeforeEach
    public void beforeEach() throws IOException {
        openMocks(this);

        when(gateway.getHost()).thenReturn("localhost");
        when(gateway.getPort()).thenReturn(8080);
        when(gateway.getScheme()).thenReturn("http");

        // simulate user-consent repo
        when(userConsentRepository.saveAndFlush(any())).then(invocation -> {
            UserConsent consent = invocation.getArgument(0);
            if (consent.getId() == null) {
                consent.setId(UUID.randomUUID());
            }
            return consent;
        });
        when(userConsentRepository.save(any())).then(invocation -> {
            UserConsent consent = invocation.getArgument(0);
            if (consent.getId() == null) {
                consent.setId(UUID.randomUUID());
            }
            return consent;
        });

        when(configuration.consentTimeout()).thenReturn(Duration.ofMinutes(5));

        when(railProviderApi.getProviderId()).thenReturn(RailProvider.NORDIGEN);
        when(railProviderFactory.get(any())).thenReturn(railProviderApi);
    }

    @Test
    public void testListConsents() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user holds several consents
        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId),
            TestData.mockUserConsent(userId),
            TestData.mockUserConsent(userId)
        );
        when(userConsentRepository.findByUserId(eq(userId), anyInt(), anyInt()))
            .thenReturn(Page.of(consents));

        // when: the fixture is called
        Page<UserConsent> result = fixture.listConsents(userId, 0, 20);

        // then: the repository is called with given parameters
        verify(userConsentRepository).findByUserId(userId, 0, 20);

        // and: the consent records are returned
        assertNotNull(result);
        assertEquals(consents.size(), result.getContentSize());
        assertEquals(consents.size(), result.getTotalCount());
        assertEquals(1, result.getTotalPages());
        assertEquals(0, result.getPageIndex());

        // and: the records are as expected
        consents.forEach(expected ->
            assertNotNull(result.stream()
                .filter(c -> c.getId().equals(expected.getId()))
                .findFirst().orElse(null))
        );
    }

    @Test
    public void testGetUserConsent_ById() {
        // given: a consent exists
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        when(userConsentRepository.findByIdOptional(consent.getId()))
            .thenReturn(Optional.of(consent));

        // when: the service is called
        Optional<UserConsent> result = fixture.getUserConsent(consent.getId());

        // then: the repository is called with the given parameters
        verify(userConsentRepository).findByIdOptional(consent.getId());

        // and: the identified consent is returned
        assertTrue(result.isPresent());
        assertEquals(consent.getId(), result.get().getId());
    }

    @Test
    public void testGetUserConsent_ById_NotFound() {
        // given: NO consent exists with the given identifier
        UUID consentId = UUID.randomUUID();
        when(userConsentRepository.findByIdOptional(consentId))
            .thenReturn(Optional.empty());

        // when: the service is called
        Optional<UserConsent> result = fixture.getUserConsent(consentId);

        // then: the repository is called with the given parameters
        verify(userConsentRepository).findByIdOptional(consentId);

        // and: NO consent is returned
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLockUserConsent() {
        // given: a consent exists
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        when(userConsentRepository.lock(consent.getId()))
            .thenReturn(Optional.of(consent));

        // when: the service is called
        Optional<UserConsent> result = fixture.lockUserConsent(consent.getId());

        // then: the repository is called with the given parameters
        verify(userConsentRepository).lock(consent.getId());

        // and: the identified consent is returned
        assertTrue(result.isPresent());
        assertEquals(consent.getId(), result.get().getId());
    }

    @Test
    public void testLockUserConsent_NotFound() {
        // given: a consent exists
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        when(userConsentRepository.lock(consent.getId()))
            .thenReturn(Optional.empty());

        // when: the service is called
        Optional<UserConsent> result = fixture.lockUserConsent(consent.getId());

        // then: the repository is called with the given parameters
        verify(userConsentRepository).lock(consent.getId());

        // and: NO consent is returned
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRegister_HappyPath_NoExistingConsent() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: no active consent exists for the identified user
        UserConsent existingConsent = TestData.mockUserConsent(userId, builder -> {
            builder.institutionId(institution.getId());
            builder.status(ConsentStatus.CANCELLED);
        });
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(Optional.of(existingConsent));

        // and: an agreement will be created
        AtomicReference<RailAgreement> agreement = new AtomicReference<>();
        when(railProviderApi.register(any(), any(), any(), any())).then(invocation -> {
            RailInstitution i = invocation.getArgument(1);
            agreement.set(returnEndUserAgreement(i));
            return agreement.get();
        });

        // when: the service is called
        URI requisitionUri = fixture.register(userId, institution.getId(), callbackUri);

        // then: an agreement is created
        assertNotNull(agreement.get());

        // and: the consent record is created
        ArgumentCaptor<UserConsent> consentCaptor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).saveAndFlush(consentCaptor.capture());
        UserConsent newConsent = consentCaptor.getValue();

        // and: the consent contains expected values
        assertEquals(ConsentStatus.INITIATED, newConsent.getStatus());
        assertEquals(userId, newConsent.getUserId());
        assertEquals(callbackUri.toString(), newConsent.getCallbackUri());
        assertEquals(institution.getId(), newConsent.getInstitutionId());
        assertEquals(agreement.get().getId(), newConsent.getAgreementId());
        assertEquals(agreement.get().getMaxHistory(), newConsent.getMaxHistory());
        assertEquals(agreement.get().getDateExpires(), newConsent.getAgreementExpires());

        // and: the initiated consent event is issued
        verify(consentEventSender).sendConsentInitiated(newConsent);

        // and: the consent timeout task was scheduled
        verify(consentTimeoutAdhocTask).queueTask(newConsent, configuration.consentTimeout());

        // and: the result is the requisition link URI
        assertEquals(requisitionUri, agreement.get().getAgreementLink());
    }

    @Test
    public void testRegister_HappyPath_ExpiredConsent() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: no active consent exists for the identified user
        UserConsent expiredConsent = TestData.mockUserConsent(userId);
        expiredConsent.setInstitutionId(institution.getId());
        expiredConsent.setStatus(ConsentStatus.EXPIRED);

        UserConsent existingConsent = TestData.mockUserConsent(userId, consent -> {
            consent.institutionId(institution.getId());
            consent.status(ConsentStatus.CANCELLED);
        });
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(Optional.of(existingConsent));

        // and: an agreement will be created
        AtomicReference<RailAgreement> agreement = new AtomicReference<>();
        when(railProviderApi.register(any(), any(), any(), any())).then(invocation -> {
            RailInstitution i = invocation.getArgument(1);
            agreement.set(returnEndUserAgreement(i));
            return agreement.get();
        });

        // when: the service is called
        URI requisitionUri = fixture.register(userId, institution.getId(), callbackUri);

        // then: an agreement is created
        assertNotNull(agreement.get());

        // and: the consent record is created
        ArgumentCaptor<UserConsent> consentCaptor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).saveAndFlush(consentCaptor.capture());
        UserConsent consent = consentCaptor.getValue();

        // and: the consent contains expected values
        assertEquals(ConsentStatus.INITIATED, consent.getStatus());
        assertEquals(userId, consent.getUserId());
        assertEquals(callbackUri.toString(), consent.getCallbackUri());
        assertEquals(institution.getId(), consent.getInstitutionId());
        assertEquals(agreement.get().getId(), consent.getAgreementId());
        assertEquals(agreement.get().getMaxHistory(), consent.getMaxHistory());
        assertEquals(agreement.get().getDateExpires(), consent.getAgreementExpires());

        // and: the initiated consent event is issued
        verify(consentEventSender).sendConsentInitiated(consent);

        // and: the consent timeout task was scheduled
        verify(consentTimeoutAdhocTask).queueTask(consent, configuration.consentTimeout());

        // and: the result is the requisition link URI
        assertEquals(requisitionUri, agreement.get().getAgreementLink());
    }

    @Test
    public void testRegister_InstitutionNotFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.empty());

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // when: the service is called
        // then: a not-found exception is thrown
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            fixture.register(userId, institution.getId(), callbackUri)
        );

        // and: the exception identifies requested institution
        assertEquals("Institution", exception.getParameter("entity-type"));
        assertEquals(institution.getId(), exception.getParameter("entity-id"));

        // and: no agreements are created
        verifyNoInteractions(railProviderApi);

        /// and: no consent is created or updated
        verify(userConsentRepository, never()).saveAndFlush(any());
        verify(userConsentRepository, never()).save(any());

        // and: no event is issued
        verifyNoInteractions(consentEventSender);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"EXPIRED", "SUSPENDED", "DENIED", "TIMEOUT", "CANCELLED"})
    public void testRegister_AlreadyRegistered(ConsentStatus consentStatus) {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: a consent exists for the identified user
        UserConsent existingConsent = TestData.mockUserConsent(userId);
        existingConsent.setInstitutionId(institution.getId());
        existingConsent.setStatus(consentStatus);
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(Optional.of(existingConsent));

        // when: the service is called
        // then: a BankAlreadyRegistered exception is thrown
        BankAlreadyRegisteredException exception = assertThrows(BankAlreadyRegisteredException.class, () ->
            fixture.register(userId, institution.getId(), callbackUri)
        );

        // and: the exception identifies user and requested institution
        assertEquals(userId, exception.getParameter("user-id"));
        assertEquals(institution.getId(), exception.getParameter("institution-id"));

        // and: no agreements are created
        verifyNoInteractions(railProviderApi);

        /// and: no consent is created or updated
        verify(userConsentRepository, never()).saveAndFlush(any());
        verify(userConsentRepository, never()).save(any());

        // and: no event is issued
        verifyNoInteractions(consentEventSender);

        // and: NO consent timeout task was scheduled
        verifyNoInteractions(consentTimeoutAdhocTask);
    }

    @Test
    public void testRegister_ExceptionRaisedByRailProvider() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: no active consent exists for the identified user
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(Optional.empty());

        // and: the rail API throws an exception
        when(railProviderApi.register(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Mock Exception"));

        // when: the service is called
        // then: a BankRegistrationException exception is thrown
        BankRegistrationException exception = assertThrows(BankRegistrationException.class, () ->
            fixture.register(userId, institution.getId(), callbackUri)
        );

        // and: the exception identifies user and requested institution
        assertEquals(userId, exception.getParameter("user-id"));
        assertEquals(institution.getId(), exception.getParameter("institution-id"));

        // and: the rail provider was called with the expected parameters
        verify(railProviderApi).register(eq(userId), eq(institution), any(), anyString());

        /// and: no consent is created or updated
        verify(userConsentRepository, never()).saveAndFlush(any());
        verify(userConsentRepository, never()).save(any());

        // and: no event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testRegister_ExceptionRaisedByService() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: no active consent exists for the identified user
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(Optional.empty());

        // and: an agreement will be created
        AtomicReference<RailAgreement> agreement = new AtomicReference<>();
        when(railProviderApi.register(any(), any(), any(), any())).then(invocation -> {
            RailInstitution i = invocation.getArgument(1);
            agreement.set(returnEndUserAgreement(i));
            return agreement.get();
        });

        // and: the consent fails to save
        doThrow(new RuntimeException("Mock Exception"))
            .when(consentEventSender).sendConsentInitiated(any());

        // when: the service is called
        // then: a BankRegistrationException exception is thrown
        BankRegistrationException exception = assertThrows(BankRegistrationException.class, () ->
            fixture.register(userId, institution.getId(), callbackUri)
        );

        // and: the exception identifies user and requested institution
        assertEquals(userId, exception.getParameter("user-id"));
        assertEquals(institution.getId(), exception.getParameter("institution-id"));

        // and: the rail provider was called with the expected parameters
        verify(railProviderApi).register(eq(userId), eq(institution), any(), anyString());

        // and: the agreement is deleted from the rail
        verify(railProviderApi).deleteAgreement(agreement.get().getId());
    }

    @Test
    public void testRegistrationTimeout_HappyPath() {
        // given: a consent that is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID(), builder -> {
            builder.status(ConsentStatus.INITIATED);
            builder.callbackUri(clientCallbackUri.toString());
        });
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.registrationTimeout(consent.getId(), consent.getReference());

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());

        // and: the consent status is set to TIMEOUT
        UserConsent updatedConsent = captor.getValue();
        assertEquals(ConsentStatus.TIMEOUT, updatedConsent.getStatus());
        assertNull(consent.getCallbackUri());

        // and: the consent event is issued
        verify(consentEventSender).sendConsentTimedOut(updatedConsent);

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(consent.getAgreementId());
    }

    @Test
    public void testRegistrationTimeout_RailException() {
        // given: a consent that is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID(), builder -> {
            builder.status(ConsentStatus.INITIATED);
            builder.callbackUri(clientCallbackUri.toString());
        });
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // and: the rail provider throws an exception on deleting agreement
        // the consent will still be timed-out
        when(railProviderApi.deleteAgreement(any())).thenThrow(new RuntimeException("Mock Exception"));

        // when: the service is called
        fixture.registrationTimeout(consent.getId(), consent.getReference());

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());

        // and: the consent status is set to TIMEOUT
        UserConsent updatedConsent = captor.getValue();
        assertEquals(ConsentStatus.TIMEOUT, updatedConsent.getStatus());
        assertNull(consent.getCallbackUri());

        // and: the consent event is issued
        verify(consentEventSender).sendConsentTimedOut(updatedConsent);

        // and: the delete agreement was called
        verify(railProviderApi).deleteAgreement(consent.getAgreementId());
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"INITIATED"})
    public void testRegistrationTimeout_NoLongerWaiting(ConsentStatus status) {
        // given: a consent that is waiting to be accepted
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID(), builder -> builder.status(status));
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.registrationTimeout(consent.getId(), consent.getReference());

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);

        // and: NO agreement is deleted
        verifyNoInteractions(railProviderApi);
    }

    @Test
    public void testRegistrationTimeout_ConsentNotFound() {
        // given: an ID for an unknown consent
        UUID consentId = UUID.randomUUID();
        when(userConsentRepository.findByIdOptional(consentId)).thenReturn(Optional.empty());

        // when: the service is called
        fixture.registrationTimeout(consentId, RandomStringUtils.insecure().nextAlphanumeric(30));

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);

        // and: NO agreement is deleted
        verifyNoInteractions(railProviderApi);
    }

    @Test
    public void testRegistrationTimeout_OutOfDateReference() {
        // given: a consent that is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID(), builder -> {
            builder.status(ConsentStatus.INITIATED);
            builder.callbackUri(clientCallbackUri.toString());
        });
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called - with a difference reference
        fixture.registrationTimeout(consent.getId(), RandomStringUtils.insecure().nextAlphanumeric(30));

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);

        // and: NO agreement is deleted
        verifyNoInteractions(railProviderApi);
    }

    @Test
    public void testConsentGiven_HappyPath() {
        // given: a consent that is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID(), builder -> {
            builder.status(ConsentStatus.INITIATED);
            builder.callbackUri(clientCallbackUri.toString());
        });
        when(userConsentRepository.findByReference(consent.getReference())).thenReturn(Optional.of(consent));
        consent.setStatus(ConsentStatus.INITIATED);
        consent.setCallbackUri(clientCallbackUri.toString());
        consent.setReference(UUID.randomUUID().toString());
        when(userConsentRepository.findByReference(consent.getReference())).thenReturn(Optional.of(consent));

        // when: the service is called
        ConsentResponse consentResponse = ConsentResponse.builder()
            .consentReference(consent.getReference())
            .build();
        URI result = fixture.consentGiven(railProviderApi, consentResponse);

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the consent is marked as given
        assertEquals(ConsentStatus.GIVEN, updatedConsent.getStatus());
        assertNotNull(updatedConsent.getDateGiven());

        // and: the callback uri is cleared
        assertNull(updatedConsent.getCallbackUri());

        // and: a task is queued to poll the consented account(s)
        verify(pollConsentAdhocTask).queueTask(updatedConsent.getId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentGiven(updatedConsent);

        // and: the result is the initial client call-back URI
        assertEquals(clientCallbackUri, result);
    }

    @Test
    public void testConsentGiven_ConsentNotFound() {
        // given: the identified consent cannot be found
        String consentReference = UUID.randomUUID().toString();
        when(userConsentRepository.findByReference(consentReference)).thenReturn(Optional.empty());

        // when: the service is called
        // then: a not-found exception is thrown
        RegistrationNotFoundException exception = assertThrows(RegistrationNotFoundException.class, () -> {
            ConsentResponse consentResponse = ConsentResponse.builder()
                .consentReference(consentReference)
                .build();
            fixture.consentGiven(railProviderApi, consentResponse);
        });

        // and: the exception identifies requested consent
        assertEquals(railProviderApi.getProviderId(), exception.getParameter("rail-provider"));
        assertEquals(consentReference, exception.getParameter("consent-reference"));

        // and: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO task is queued to poll the consented account(s)
        verifyNoInteractions(pollConsentAdhocTask);

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testConsentDenied_HappyPath() {
        // given: a consent that is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.INITIATED);
        consent.setCallbackUri(clientCallbackUri.toString());
        consent.setReference(UUID.randomUUID().toString());
        consent.setDateGiven(null);
        when(userConsentRepository.findByReference(consent.getReference())).thenReturn(Optional.of(consent));

        // when: the service is called
        ConsentResponse consentResponse = ConsentResponse.builder()
            .consentReference(consent.getReference())
            .errorCode("UserCancelledSession")
            .errorDescription("User Cancelled Session")
            .build();
        URI result = fixture.consentDenied(railProviderApi, consentResponse);

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the consent is marked as denied
        assertEquals(ConsentStatus.DENIED, updatedConsent.getStatus());
        assertNull(updatedConsent.getDateGiven());
        assertNotNull(updatedConsent.getDateDenied());

        // and: the callback uri is cleared
        assertNull(updatedConsent.getCallbackUri());

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentDenied(updatedConsent);

        // and: the result is the initial client call-back URI - with error params
        URI callback = UriBuilder.fromUri(clientCallbackUri)
            .queryParam("error", consentResponse.getErrorCode())
            .queryParam("details", consentResponse.getErrorDescription())
            .build();
        assertEquals(callback, result);
    }

    @Test
    public void testConsentDenied_ConsentNotFound() {
        // given: a consent that is waiting to be accepted
        String consentReference = UUID.randomUUID().toString();
        when(userConsentRepository.findByReference(consentReference)).thenReturn(Optional.empty());

        // when: the service is called
        // then: a not-found exception is thrown
        ConsentResponse consentResponse = ConsentResponse.builder()
            .consentReference(consentReference)
            .errorCode("UserCancelledSession")
            .errorDescription("User Cancelled Session")
            .build();
        RegistrationNotFoundException exception = assertThrows(RegistrationNotFoundException.class, () -> {
            fixture.consentDenied(railProviderApi, consentResponse);
        });

        // and: the exception identifies requested consent
        assertEquals(railProviderApi.getProviderId(), exception.getParameter("rail-provider"));
        assertEquals(consentResponse.getConsentReference(), exception.getParameter("consent-reference"));
        assertEquals(consentResponse.getErrorCode(), exception.getParameter("error-code"));
        assertEquals(consentResponse.getErrorDescription(), exception.getParameter("error-description"));

        // then: the consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO requisition is deleted
        verify(railProviderApi, never()).deleteAgreement(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"SUSPENDED"})
    public void testConsentSuspended_HappyPath(ConsentStatus consentStatus) {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(consentStatus);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentSuspended(consent.getId());

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the status is set to SUSPENDED
        assertEquals(ConsentStatus.SUSPENDED, updatedConsent.getStatus());

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentSuspended(updatedConsent);
    }

    @Test
    public void testConsentSuspended_AlreadySuspended() {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.SUSPENDED);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentSuspended(consent.getId());

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testConsentSuspended_NotFound() {
        // given: a consent identifier
        UUID consentId = UUID.randomUUID();
        when(userConsentRepository.findByIdOptional(consentId)).thenReturn(Optional.empty());

        // when: the service is called
        fixture.consentSuspended(consentId);

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"EXPIRED"})
    public void testConsentExpired_HappyPath(ConsentStatus consentStatus) {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(consentStatus);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentExpired(consent.getId());

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the status is set to EXPIRED
        assertEquals(ConsentStatus.EXPIRED, updatedConsent.getStatus());

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentExpired(updatedConsent);
    }

    @Test
    public void testConsentExpired_AlreadyExpired() {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.EXPIRED);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentExpired(consent.getId());

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testConsentExpired_NotFound() {
        // given: a consent identifier
        UUID consentId = UUID.randomUUID();
        when(userConsentRepository.findByIdOptional(consentId)).thenReturn(Optional.empty());

        // when: the service is called
        fixture.consentSuspended(consentId);

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"CANCELLED"})
    public void testConsentCancelled_HappyPath(ConsentStatus consentStatus) {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(consentStatus);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called - and the purge flag is NOT set
        fixture.consentCancelled(consent.getId(), false);

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the status is set to CANCELLED
        assertEquals(ConsentStatus.CANCELLED, updatedConsent.getStatus());
        assertNotNull(updatedConsent.getDateCancelled());

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentCancelled(updatedConsent);

        // and: NO consent is deleted
        verify(userConsentRepository, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = {"CANCELLED"})
    public void testConsentCancelled_Purge_HappyPath(ConsentStatus consentStatus) {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(consentStatus);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called - and the purge flag is set
        fixture.consentCancelled(consent.getId(), true);

        // then: the consent is deleted
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).delete(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the status is set to CANCELLED
        assertEquals(ConsentStatus.CANCELLED, updatedConsent.getStatus());
        assertNotNull(updatedConsent.getDateCancelled());

        // and: the agreement is deleted
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentCancelled(updatedConsent);

        // and: NO consent is updated
        verify(userConsentRepository, never()).save(any());
    }

    @Test
    public void testConsentCancelled_AlreadyCancelled() {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.CANCELLED);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentCancelled(consent.getId(), false);

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testConsentCancelled_NotFound() {
        // given: a consent identifier
        UUID consentId = UUID.randomUUID();
        when(userConsentRepository.findByIdOptional(consentId)).thenReturn(Optional.empty());

        // when: the service is called
        fixture.consentCancelled(consentId, false);

        // then: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testDeleteAllConsents() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: the user holds several consents
        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.SUSPENDED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.CANCELLED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.EXPIRED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.DENIED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.GIVEN)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.INITIATED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.INITIATED)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.GIVEN)),
            TestData.mockUserConsent(userId, consent -> consent.status(ConsentStatus.GIVEN))
        );
        when(userConsentRepository.findByUserId(eq(userId)))
            .thenReturn(consents);

        // when: the fixture is called
        fixture.deleteAllConsents(userId);

        // then: the repository is called
        verify(userConsentRepository).findByUserId(userId);

        // and: the consents are all deleted
        consents.forEach(consent ->
            verify(userConsentRepository).delete(consent)
        );

        consents.forEach(consent -> {
            if ((consent.getStatus() == ConsentStatus.SUSPENDED) ||
                (consent.getStatus() == ConsentStatus.CANCELLED) ||
                (consent.getStatus() == ConsentStatus.EXPIRED) ||
                (consent.getStatus() == ConsentStatus.DENIED)) {
                // and: NO non-active agreements are deleted
                verify(railProviderApi, never()).deleteAgreement(consent.getAgreementId());
                // and: NO consent events are issued for non-active consent
                verify(consentEventSender, never()).sendConsentCancelled(consent);
            } else {
                // and: all active agreements are deleted
                verify(railProviderApi).deleteAgreement(consent.getAgreementId());
                // and: consent events are issued for each active consent
                verify(consentEventSender).sendConsentCancelled(consent);
            }
        });
    }

    @Test
    public void testDeleteAgreement_Exception() {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.GIVEN);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // and: the rail service will throw an exception on delete agreement
        when(railProviderApi.deleteAgreement(any()))
            .thenThrow(new RuntimeException("some rail exception"));

        // when: the consent is expired
        // then: exception is thrown
        DeleteRailConsentException exception = assertThrows(DeleteRailConsentException.class, () ->
            fixture.consentExpired(consent.getId())
        );

        // and: the exception identifies the rail provider and consent
        assertEquals(consent.getProvider(), exception.getParameter("rail-provider"));
        assertEquals(consent.getId(), exception.getParameter("consent-id"));

        // and: the rail is called to delete the agreement
        verify(railProviderApi).deleteAgreement(consent.getAgreementId());

        // and: NO consent event is issued
        verify(consentEventSender, never()).sendConsentExpired(any());
    }

    private RailAgreement returnEndUserAgreement(RailInstitution institution) {
        return RailAgreement.builder()
            .id(UUID.randomUUID().toString())
            .accountIds(List.of(insecure().nextAlphanumeric(30), insecure().nextAlphanumeric(30)))
            .status(AgreementStatus.INITIATED)
            .dateCreated(Instant.now())
            .dateGiven(Instant.now())
            .dateExpires(Instant.now().plus(Duration.ofDays(90)))
            .institutionId(institution.getId())
            .maxHistory(institution.getTransactionTotalDays())
            .agreementLink(URI.create("http://mock-agreement-link"))
            .build();
    }
}
