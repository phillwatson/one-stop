package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.user.domain.MagicToken;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.MagicTokenRepository;
import com.hillayes.user.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.user.utils.TestData.mockUser;
import static com.hillayes.user.utils.TestData.mockUsers;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class UserServiceTest {
    @InjectMock
    UserRepository userRepository;

    @InjectMock
    DeletedUserRepository deletedUserRepository;

    @InjectMock
    MagicTokenRepository magicTokenRepository;

    @InjectMock
    PasswordCrypto passwordCrypto;

    @InjectMock
    UserEventSender userEventSender;

    @Inject
    UserService fixture;

    @BeforeEach
    public void beforeEach() {
        // simulate a successful password hash
        when(passwordCrypto.getHash(any())).thenReturn(randomAlphanumeric(20));

        // simulate the user repository returns the user with an ID
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user = user.toBuilder()
                    .id(UUID.randomUUID())
                    .build();
            }
            return user;
        });

        // simulate the token repository returns the token with an ID
        when(magicTokenRepository.save(any())).thenAnswer(invocation -> {
            MagicToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token = token.toBuilder()
                    .id(UUID.randomUUID())
                    .build();
            }
            return token;
        });
    }

    @Test
    public void testRegisterUser() throws IOException {
        // given: an email address
        String email = randomAlphanumeric(10) + "@example.com";

        // when: we register a user
        fixture.registerUser(email);

        // then: a token is registered
        ArgumentCaptor<MagicToken> tokenCaptor = ArgumentCaptor.forClass(MagicToken.class);
        verify(userEventSender).sendUserRegistered(tokenCaptor.capture(), any());

        // and: the token records the email
        MagicToken token = tokenCaptor.getValue();
        assertEquals(email.toLowerCase(), token.getEmail());

        // and: the token contains a random code
        assertNotNull(token.getToken());

        // and: the token expires at some future time
        assertTrue(token.getExpires().isAfter(Instant.now()));
    }

    @Test
    public void testRegisterUser_DuplicateEmail() {
        // given: an email address
        String email = randomAlphanumeric(10) + "@example.com";

        // and: a user with the given email already exists
        when(userRepository.findByEmail(email.toLowerCase()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // and: NO registered user with the given email already exists
        when(magicTokenRepository.findByEmail(email.toLowerCase()))
            .thenReturn(Optional.empty());

        // when: registering a user - an exception is thrown
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.registerUser(email)
        );

        // then: NO token is registered
        verify(userEventSender, never()).sendUserRegistered(any(), any());
    }

    @Test
    public void testRegisterUser_DuplicateEmailRegistered() {
        // given: an email address
        String email = randomAlphanumeric(10) + "@example.com";

        // and: NO onboarded user with the given email already exists
        when(userRepository.findByEmail(email.toLowerCase()))
            .thenReturn(Optional.empty());

        // and: a registered user with the given email already exists
        when(magicTokenRepository.findByEmail(email.toLowerCase()))
            .thenReturn(Optional.of(MagicToken.builder().id(UUID.randomUUID()).build()));

        // when: registering a user - an exception is thrown
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.registerUser(email)
        );

        // then: NO token is registered
        verify(userEventSender, never()).sendUserRegistered(any(), any());
    }

    @Test
    public void testAcknowledgeToken() {
        // given: a token
        String token = randomAlphanumeric(30);

        // and: the token repository contains the token
        MagicToken entry = MagicToken.builder()
            .email(randomAlphanumeric(10) + "@example.com")
            .token(token)
            .expires(Instant.now().plusSeconds(60))
            .build();
        when(magicTokenRepository.findByToken(token)).thenReturn(Optional.of(entry));

        // when: acknowledging the token
        Optional<User> user = fixture.acknowledgeToken(token);

        // then: a new user is returned
        assertTrue(user.isPresent());

        // and: the user is not yet onboarded
        assertFalse(user.get().isOnboarded());

        // and: the user is persisted
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        // and: the persisted user has a role
        User createdUser = userCaptor.getValue();
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().contains("onboarding"));

        // and: the token is deleted
        verify(magicTokenRepository).delete(entry);

        // and: the user acknowledgement is issued
        verify(userEventSender).sendUserAcknowledged(user.get());
    }

    @Test
    public void testAcknowledgeToken_TokenNotFound() {
        // given: a token
        String token = randomAlphanumeric(30);

        // and: the token repository DOES NOT contain the token
        when(magicTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // when: acknowledging the token
        Optional<User> user = fixture.acknowledgeToken(token);

        // then: NO new user is returned
        assertFalse(user.isPresent());

        // and: NO user is created
        verify(userRepository, never()).save(any());

        // and: NO token is deleted
        verify(magicTokenRepository, never()).delete(any());

        // and: NO user acknowledgement is issued
        verify(userEventSender, never()).sendUserAcknowledged(any());
    }

    @Test
    public void testAcknowledgeToken_TokenIsExpired() {
        // given: a token
        String token = randomAlphanumeric(30);

        // and: the token repository contains the expired token
        MagicToken entry = MagicToken.builder()
            .email(randomAlphanumeric(10) + "@example.com")
            .token(token)
            .expires(Instant.now().minusSeconds(60))
            .build();
        when(magicTokenRepository.findByToken(token)).thenReturn(Optional.of(entry));

        // when: acknowledging the token
        Optional<User> user = fixture.acknowledgeToken(token);

        // then: NO new user is returned
        assertFalse(user.isPresent());

        // and: NO user is created
        verify(userRepository, never()).save(any());

        // and: NO token is deleted
        verify(magicTokenRepository, never()).delete(any());

        // and: NO user acknowledgement is issued
        verify(userEventSender, never()).sendUserAcknowledged(any());
    }

    @Test
    public void testCompleteOnboarding() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // when: completing the onboarding
        Optional<User> onboardedUser = fixture.completeOnboarding(userId, user, password);

        // then: the user is returned
        assertTrue(onboardedUser.isPresent());

        // and: the user is onboarded
        assertTrue(onboardedUser.get().isOnboarded());

        // and: the user was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        // and: the saved user contains new properties
        User savedUser = userCaptor.getValue();
        assertEquals(user.getUsername(), savedUser.getUsername());
        assertEquals(user.getEmail().toLowerCase(), savedUser.getEmail());
        assertEquals(user.getPreferredName(), savedUser.getPreferredName());
        assertEquals(user.getTitle(), savedUser.getTitle());
        assertEquals(user.getGivenName(), savedUser.getGivenName());
        assertEquals(user.getFamilyName(), savedUser.getFamilyName());
        assertEquals(user.getPhoneNumber(), savedUser.getPhoneNumber());
        assertNotNull(user.getPasswordHash());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains("user"));

        // and: a notification is issued
        verify(userEventSender).sendUserCreated(onboardedUser.get());
    }

    @Test
    public void testCompleteOnboarding_MissingUsername() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .username(null)
            .build();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: an existing user holds the same username
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // then: the username is the missing parameter
        assertEquals("username", exception.getParameter("parameter-name"));

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_MissingEmail() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .email(null)
            .build();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: an existing user holds the same username
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // and: the email is the missing parameter
        assertEquals("email", exception.getParameter("parameter-name"));

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_MissingGivenName() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .givenName(null)
            .build();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: an existing user holds the same username
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // and: the givenName is the missing parameter
        assertEquals("givenName", exception.getParameter("parameter-name"));

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_DuplicateUsername() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: an existing user holds the same username
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        assertThrows(DuplicateUsernameException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_DuplicateEmail() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: an existing user holds the same email
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // and: NO registered user with the given email already exists
        when(magicTokenRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // when: completing the onboarding
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_DuplicateEmailRegistered() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));

        // and: NO onboarded user with the given email already exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // and: a registered user with the given email already exists
        when(magicTokenRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(MagicToken.builder().id(UUID.randomUUID()).build()));

        // when: completing the onboarding
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.completeOnboarding(userId, user, password)
        );

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testGetUser() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // when: the service is called
        Optional<User> result = fixture.getUser(user.getId());

        // then: the user is returned
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    public void testGetUser_NotFound() {
        // given: no user with the given ID
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when: the service is called
        Optional<User> result = fixture.getUser(userId);

        // then: the result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListUsers() {
        // given: a list of users
        List<User> users = mockUsers(20);

        // and: the user repository returns the users
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        // when: the service is called
        Page<User> result = fixture.listUsers(1, 20);

        // then: the users are returned
        assertEquals(users.size(), result.getNumberOfElements());
    }

    @Test
    public void testUpdatePassword() {
        // given: a user
        User user = mockUser(UUID.randomUUID()).toBuilder()
            .passwordHash("oldhash")
            .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: the given old password is correct
        when(passwordCrypto.verify("oldpassword".toCharArray(), user.getPasswordHash())).thenReturn(true);

        // when: the service is called
        Optional<User> result = fixture.updatePassword(user.getId(), "oldpassword".toCharArray(), "newpassword".toCharArray());

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the password is hashed
        verify(passwordCrypto).getHash("newpassword".toCharArray());

        // and: the user is saved
        verify(userRepository).save(any());

        // and: an event is sent
        verify(userEventSender).sendUserUpdated(result.get());
    }

    @Test
    public void testUpdatePassword_NotFound() {
        // given: a user
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when: the service is called
        Optional<User> result = fixture.updatePassword(userId, "oldpassword".toCharArray(), "newpassword".toCharArray());

        // then: the user is NOT returned
        assertFalse(result.isPresent());

        // and: the password is NOT hashed
        verify(passwordCrypto, never()).getHash(any());

        // and: the user is NOT saved
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdatePassword_WrongOldPassword() {
        // given: a user
        User user = mockUser(UUID.randomUUID()).toBuilder()
            .passwordHash("oldhash")
            .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: the given old password is NOT correct
        when(passwordCrypto.verify("oldpassword".toCharArray(), user.getPasswordHash())).thenReturn(false);

        // when: the service is called
        Optional<User> result = fixture.updatePassword(user.getId(), "oldpassword".toCharArray(), "newpassword".toCharArray());

        // then: the user is NOT returned
        assertFalse(result.isPresent());

        // and: the password is NOT hashed
        verify(passwordCrypto, never()).getHash(any());

        // and: the user is NOT saved
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: no other user with the given username exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(user));

        // and: no other user with the given email exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(user));

        // and: NO onboarding user with the given email exists
        when(magicTokenRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // when: the service is called
        Optional<User> result = fixture.updateUser(user.getId(), modifiedUser);

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the user is updated
        assertEquals(user.getId(), result.get().getId());
        assertEquals(modifiedUser.getGivenName(), result.get().getGivenName());
        assertEquals(modifiedUser.getFamilyName(), result.get().getFamilyName());

        // and: the user is saved
        verify(userRepository).save(any());

        // and: an event is sent
        verify(userEventSender).sendUserUpdated(result.get());
    }

    @Test
    public void testUpdateUser_NotFound() {
        // given: an unknown user
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // and: an update request
        User modifiedUser = mockUser();

        // when: the service is called
        Optional<User> result = fixture.updateUser(userId, modifiedUser);

        // then: no user is returned
        assertFalse(result.isPresent());

        // and: the user is NOT saved
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_MissingUsername() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .username(null)
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the username is the missing parameter
        assertEquals("username", exception.getParameter("parameter-name"));

        // and: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_MissingEmail() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .email(null)
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the email is the missing parameter
        assertEquals("email", exception.getParameter("parameter-name"));

        // and: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_MissingGivenName() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .givenName(null)
            .familyName("New Last Name")
            .build();

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the givenName is the missing parameter
        assertEquals("givenName", exception.getParameter("parameter-name"));

        // and: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_DuplicateUsername() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        assertThrows(DuplicateUsernameException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_DuplicateEmail() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: a user with the given email already exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // and: NO registered user with the given email already exists
        when(magicTokenRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // when: updating a user - an exception is thrown
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdateUser_DuplicateEmail_Registered() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // and: an update request
        User modifiedUser = user.toBuilder()
            .givenName("New First Name")
            .familyName("New Last Name")
            .build();

        // and: NO onboarded user with the given email already exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.empty());

        // and: a registered user with the given email already exists
        when(magicTokenRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(MagicToken.builder().id(UUID.randomUUID()).build()));

        // when: updating a user - an exception is thrown
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.updateUser(user.getId(), modifiedUser)
        );

        // then: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testDeleteUser() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // when: the service is called
        Optional<User> result = fixture.deleteUser(user.getId());

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the user is deleted
        verify(userRepository).delete(user);

        // and: the deleted user is saved to the deleted user repository
        verify(deletedUserRepository).save(any());

        // and: an event is sent
        verify(userEventSender).sendUserDeleted(any());
    }

    @Test
    public void testDeleteUser_NotFound() {
        // given: an unknown user
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when: the service is called
        Optional<User> result = fixture.deleteUser(userId);

        // then: the user is NOT returned
        assertFalse(result.isPresent());

        // and: NO user is deleted
        verify(userRepository, never()).delete(any());

        // and: NO deleted user is saved to the deleted user repository
        verify(deletedUserRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserDeleted(any());
    }
}
