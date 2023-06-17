package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import jakarta.inject.Inject;
import java.io.IOException;
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
    PasswordCrypto passwordCrypto;

    @InjectMock
    AuthTokens authTokens;

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
    }

    @Test
    public void testRegisterUser() throws IOException {
        // given: an email address
        String email = randomAlphanumeric(10) + "@example.com";

        // and: a token is generated
        String token = randomAlphanumeric(30);
        when(authTokens.generateToken(eq(email.toLowerCase()), any())).thenReturn(token);

        // when: we register a user
        fixture.registerUser(email);

        // then: a token is generated
        verify(authTokens).generateToken(eq(email.toLowerCase()), any());

        // and: a registration is recorded
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(userEventSender).sendUserRegistered(tokenCaptor.capture(), any(), any());
    }

    @Test
    public void testRegisterUser_DuplicateEmail() throws IOException {
        // given: an email address
        String email = randomAlphanumeric(10) + "@example.com";

        // and: an existing user holds the same email
        User existingUser = mockUser(UUID.randomUUID());
        when(userRepository.findByEmail(email.toLowerCase()))
            .thenReturn(Optional.of(existingUser));

        // when: we register a user
        fixture.registerUser(email);

        // then: No registration is recorded
        verify(userEventSender, never()).sendUserRegistered(any(), any(), any());

        // and: a warning email is sent to the existing user with the same email
        verify(userEventSender).sendAccountActivity(existingUser, SuspiciousActivity.EMAIL_REGISTRATION);
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
        User onboardedUser = fixture.completeOnboarding(user, password);

        // then: the user is returned
        assertNotNull(onboardedUser);

        // and: the user is onboarded
        assertTrue(onboardedUser.isOnboarded());

        // and: the user was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        // and: the saved user contains new properties
        User savedUser = userCaptor.getValue();
        assertEquals(user.getUsername(), savedUser.getUsername());
        assertEquals(user.getEmail().toLowerCase(), savedUser.getEmail().toLowerCase());
        assertEquals(user.getPreferredName(), savedUser.getPreferredName());
        assertEquals(user.getTitle(), savedUser.getTitle());
        assertEquals(user.getGivenName(), savedUser.getGivenName());
        assertEquals(user.getFamilyName(), savedUser.getFamilyName());
        assertEquals(user.getPhoneNumber(), savedUser.getPhoneNumber());
        assertNotNull(user.getPasswordHash());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains("user"));

        // and: a notification is issued
        verify(userEventSender).sendUserCreated(onboardedUser);
    }

    @Test
    public void testCompleteOnboarding_MissingUsername() {
        // given: a completed user request
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .username(null)
            .build();

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(user, password)
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
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .email(null)
            .build();

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(user, password)
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
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser().toBuilder()
            .givenName(null)
            .build();

        // when: completing the onboarding
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.completeOnboarding(user, password)
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
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: an existing user holds the same username
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        assertThrows(DuplicateUsernameException.class, () ->
            fixture.completeOnboarding(user, password)
        );

        // and: NO user is updated
        verify(userRepository, never()).save(any());

        // and: NO notification is issued
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCompleteOnboarding_DuplicateEmail() {
        // given: a completed user request
        char[] password = randomAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: an existing user holds the same email
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: completing the onboarding
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.completeOnboarding(user, password)
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
