package com.hillayes.rail.service;

import com.hillayes.commons.jpa.Page;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.RailAgreement;
import com.hillayes.rail.api.domain.AgreementStatus;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.domain.ConsentStatus;
import com.hillayes.rail.domain.UserConsent;
import com.hillayes.rail.errors.BankAlreadyRegisteredException;
import com.hillayes.rail.event.ConsentEventSender;
import com.hillayes.rail.repository.UserConsentRepository;
import com.hillayes.rail.scheduled.PollConsentJobbingTask;
import com.hillayes.rail.utils.TestApiData;
import com.hillayes.rail.utils.TestData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserConsentServiceTest {
    @InjectMock
    UserConsentRepository userConsentRepository;

    @InjectMock
    InstitutionService institutionService;

    @InjectMock
    RailProviderFactory railProviderFactory;

    @InjectMock
    PollConsentJobbingTask pollConsentJobbingTask;

    @InjectMock
    ConsentEventSender consentEventSender;

    @Inject
    UserConsentService fixture;

    private RailProviderApi railProviderApi;

    @BeforeEach
    public void beforeEach() {
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

        railProviderApi = mock();
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
    public void testGetUserConsent_ByInstitute() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        String institutionId = randomAlphanumeric(30);

        // and: the user holds several consents for the institute
        UserConsent activeConsent = TestData.mockUserConsent(userId, consent -> {
            consent.setInstitutionId(institutionId);
            consent.setStatus(ConsentStatus.GIVEN);
            return consent;
        });
        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institutionId);
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            activeConsent,
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institutionId);
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            })
        );
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId))
            .thenReturn(consents);

        // when: the service is called
        Optional<UserConsent> result = fixture.getUserConsent(userId, institutionId);

        // then: the repository is called with the given parameters
        verify(userConsentRepository).findByUserIdAndInstitutionId(userId, institutionId);

        // and: the non-cancelled, non-denied consent is returned
        assertTrue(result.isPresent());
        assertEquals(activeConsent.getId(), result.get().getId());
    }

    @Test
    public void testGetUserConsent_ByInstitute_NonFound() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        String institutionId = randomAlphanumeric(30);

        // and: the user holds NO consents for the institute
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId))
            .thenReturn(List.of());

        // when: the service is called
        Optional<UserConsent> result = fixture.getUserConsent(userId, institutionId);

        // then: the repository is called with the given parameters
        verify(userConsentRepository).findByUserIdAndInstitutionId(userId, institutionId);

        // and: no consent is returned
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetUserConsent_ByInstitute_NoActiveConsent() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        String institutionId = randomAlphanumeric(30);

        // and: the user holds several consents for the institute - non are active
        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institutionId);
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institutionId);
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            })
        );
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institutionId))
            .thenReturn(consents);

        // when: the service is called
        Optional<UserConsent> result = fixture.getUserConsent(userId, institutionId);

        // then: the repository is called with the given parameters
        verify(userConsentRepository).findByUserIdAndInstitutionId(userId, institutionId);

        // and: NO consent is returned
        assertTrue(result.isEmpty());
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
    public void testRegister_HappyPath_NoExistingConsent() {
        // given: a user identity
        UUID userId = UUID.randomUUID();

        // and: an identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(institutionService.get(eq(institution.getId()))).thenReturn(Optional.of(institution));

        // and: a client callback URI
        URI callbackUri = URI.create("http://mock-uri");

        // and: no active consent exists for the identified user
        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            })
        );
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(consents);

        // and: an agreement will be created
        AtomicReference<RailAgreement> agreement = new AtomicReference<>();
        when(railProviderApi.register(any(), any(), any(), any())).then(invocation -> {
            RailInstitution i = invocation.getArgument(1);
            URI uri = invocation.getArgument(2);
            String reference = invocation.getArgument(3);
            agreement.set(returnEndUserAgreement(reference, i, uri));
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
        assertEquals(ConsentStatus.WAITING, consent.getStatus());
        assertEquals(userId, consent.getUserId());
        assertEquals(callbackUri.toString(), consent.getCallbackUri());
        assertEquals(institution.getId(), consent.getInstitutionId());
        assertEquals(agreement.get().getId(), consent.getAgreementId());
        assertEquals(agreement.get().getMaxHistory(), consent.getMaxHistory());
        assertEquals(agreement.get().getDateExpires(), consent.getAgreementExpires());

        // and: the initiated consent event is issued
        verify(consentEventSender).sendConsentInitiated(consent);

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

        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            }),
            expiredConsent
        );
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(consents);

        // and: an agreement will be created
        AtomicReference<RailAgreement> agreement = new AtomicReference<>();
        when(railProviderApi.register(any(), any(), any(), any())).then(invocation -> {
            RailInstitution i = invocation.getArgument(1);
            URI uri = invocation.getArgument(2);
            String reference = invocation.getArgument(3);
            agreement.set(returnEndUserAgreement(reference, i, uri));
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
        assertEquals(ConsentStatus.WAITING, consent.getStatus());
        assertEquals(userId, consent.getUserId());
        assertEquals(callbackUri.toString(), consent.getCallbackUri());
        assertEquals(institution.getId(), consent.getInstitutionId());
        assertEquals(agreement.get().getId(), consent.getAgreementId());
        assertEquals(agreement.get().getMaxHistory(), consent.getMaxHistory());
        assertEquals(agreement.get().getDateExpires(), consent.getAgreementExpires());

        // and: the initiated consent event is issued
        verify(consentEventSender).sendConsentInitiated(consent);

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
    @EnumSource(names = {"INITIATED", "WAITING", "GIVEN", "SUSPENDED"})
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

        List<UserConsent> consents = List.of(
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setInstitutionId(institution.getId());
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            }),
            existingConsent
        );
        when(userConsentRepository.findByUserIdAndInstitutionId(userId, institution.getId()))
            .thenReturn(consents);

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
    }

    @Test
    public void testConsentGiven_HappyPath() {
        // given: a consent that has is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.WAITING);
        consent.setCallbackUri(clientCallbackUri.toString());
        consent.setReference(UUID.randomUUID().toString());
        when(userConsentRepository.findByReference(consent.getReference())).thenReturn(Optional.of(consent));

        // when: the service is called
        MultivaluedMapImpl<String, String> queryParameters = new MultivaluedMapImpl<>();
        queryParameters.add("ref", consent.getReference());
        URI result = fixture.consentGiven(consent.getProvider(), queryParameters);

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the consent is marked as given
        assertEquals(ConsentStatus.GIVEN, updatedConsent.getStatus());
        assertNotNull(updatedConsent.getDateGiven());

        // and: the callback uri is cleared
        assertNull(updatedConsent.getCallbackUri());

        // and: a job is queued to poll the consented account(s)
        verify(pollConsentJobbingTask).queueJob(updatedConsent.getId());

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
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            MultivaluedMapImpl<String, String> queryParameters = new MultivaluedMapImpl<>();
            queryParameters.add("ref", consentReference);
            fixture.consentGiven(RailProvider.NORDIGEN, queryParameters);
        });

        // and: the exception identifies requested consent
        assertEquals("UserConsent.reference", exception.getParameter("entity-type"));
        assertEquals(consentReference, exception.getParameter("entity-id"));

        // and: NO consent is updated
        verify(userConsentRepository, never()).save(any());

        // and: NO job is queued to poll the consented account(s)
        verifyNoInteractions(pollConsentJobbingTask);

        // and: NO consent event is issued
        verifyNoInteractions(consentEventSender);
    }

    @Test
    public void testConsentDenied_HappyPath() {
        // given: a consent that has is waiting to be accepted
        URI clientCallbackUri = URI.create("http://mock-uri");
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.WAITING);
        consent.setCallbackUri(clientCallbackUri.toString());
        consent.setReference(UUID.randomUUID().toString());
        consent.setDateGiven(null);
        when(userConsentRepository.findByReference(consent.getReference())).thenReturn(Optional.of(consent));

        // when: the service is called
        String errorCode = "UserCancelledSession";
        String errorDetail = "User Cancelled Session";
        MultivaluedMapImpl<String, String> queryParameters = new MultivaluedMapImpl<>();
        queryParameters.add("ref", consent.getReference());
        queryParameters.add("error", errorCode);
        queryParameters.add("details", errorDetail);
        URI result = fixture.consentDenied(consent.getProvider(), queryParameters);

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
            .queryParam("error", errorCode)
            .queryParam("details", errorDetail)
            .build();
        assertEquals(callback, result);
    }

    @Test
    public void testConsentDenied_NotFound() {
        // given: a consent that has is waiting to be accepted
        String consentReference = UUID.randomUUID().toString();
        when(userConsentRepository.findByReference(consentReference)).thenReturn(Optional.empty());

        // when: the service is called
        // then: a not-found exception is thrown
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            MultivaluedMapImpl<String, String> queryParameters = new MultivaluedMapImpl<>();
            queryParameters.add("ref", consentReference);
            queryParameters.add("error", "UserCancelledSession");
            queryParameters.add("details", "User Cancelled Session");
            fixture.consentDenied(RailProvider.NORDIGEN, queryParameters);
        });

        // and: the exception identifies requested consent
        assertEquals("UserConsent.reference", exception.getParameter("entity-type"));
        assertEquals(consentReference, exception.getParameter("entity-id"));

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

        // when: the service is called
        fixture.consentCancelled(consent.getId());

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
    }

    @Test
    public void testConsentCancelled_AlreadyCancelled() {
        // given: a consent identifier
        UserConsent consent = TestData.mockUserConsent(UUID.randomUUID());
        consent.setStatus(ConsentStatus.CANCELLED);
        when(userConsentRepository.findByIdOptional(consent.getId())).thenReturn(Optional.of(consent));

        // when: the service is called
        fixture.consentCancelled(consent.getId());

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
        fixture.consentCancelled(consentId);

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
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.SUSPENDED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.CANCELLED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.EXPIRED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.DENIED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.GIVEN);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.INITIATED);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.WAITING);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.GIVEN);
                return consent;
            }),
            TestData.mockUserConsent(userId, consent -> {
                consent.setStatus(ConsentStatus.GIVEN);
                return consent;
            })
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
        fixture.consentExpired(consent.getId());

        // then: the consent is updated
        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(userConsentRepository).save(captor.capture());
        UserConsent updatedConsent = captor.getValue();

        // and: the status is set to EXPIRED
        assertEquals(ConsentStatus.EXPIRED, updatedConsent.getStatus());

        // and: the agreement is deleted - the exception is ignored
        verify(railProviderApi).deleteAgreement(updatedConsent.getAgreementId());

        // and: a consent event is issued
        verify(consentEventSender).sendConsentExpired(updatedConsent);
    }

    private RailAgreement returnEndUserAgreement(String reference, RailInstitution institution, URI callbackUri) {
        return RailAgreement.builder()
            .id(UUID.randomUUID().toString())
            .accountIds(List.of(randomAlphanumeric(30), randomAlphanumeric(30)))    
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
