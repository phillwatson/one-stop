package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.errors.UserAlreadyOnboardedException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import javax.inject.Inject;
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
    }

    @Test
    public void testCreateUser() {
        // given: a new user
        User newUser = mockUser();
        String username = randomAlphanumeric(20);
        String password = randomAlphanumeric(20);

        // when: creating a user
        User user = fixture.createUser(username, password.toCharArray(), newUser);

        // then: the user is created
        verify(userRepository).save(any());

        // and: the user is assigned an ID
        assertNotNull(user.getId());

        // and: the user is assigned a password hash
        verify(passwordCrypto).getHash(password.toCharArray());

        // and: the saved user matches the new user
        assertEquals(username, user.getUsername());
        assertEquals(newUser.getEmail(), user.getEmail());
        assertEquals(newUser.getGivenName(), user.getGivenName());
        assertEquals(newUser.getFamilyName(), user.getFamilyName());
        assertEquals(newUser.getPhoneNumber(), user.getPhoneNumber());
        assertEquals(newUser.getRoles(), user.getRoles());

        // and: an event is sent
        verify(userEventSender).sendUserCreated(user);
    }

    @Test
    public void testCreateUser_DuplicateUsername() {
        // given: a new user
        User newUser = mockUser();
        String username = randomAlphanumeric(20);
        String password = randomAlphanumeric(20);

        // and: a user with the given username already exists
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser()));

        // when: creating a user - an exception is thrown
        assertThrows(DuplicateUsernameException.class, () ->
            fixture.createUser(username, password.toCharArray(), newUser)
        );

        // then: the user is NOT created
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testCreateUser_DuplicateEmail() {
        // given: a new user
        User newUser = mockUser();
        String username = randomAlphanumeric(20);
        String password = randomAlphanumeric(20);

        // and: a user with the given username already exists
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(mockUser()));

        // when: creating a user - an exception is thrown
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.createUser(username, password.toCharArray(), newUser)
        );

        // then: the user is NOT created
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserCreated(any());
    }

    @Test
    public void testOnboardUser() {
        // given: a user that has not been onboarded
        User user = mockUser(UUID.randomUUID());
        assertNull(user.getDateOnboarded());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // when: onboarding the user
        User onboardedUser = fixture.onboardUser(user.getId())
            .orElse(null);

        // then: the user is onboarded
        assertNotNull(onboardedUser);
        assertEquals(user.getId(), onboardedUser.getId());
        assertNotNull(onboardedUser.getDateOnboarded());

        // and: the user is saved
        verify(userRepository).save(any());

        // and: an event is sent
        verify(userEventSender).sendUserOnboarded(onboardedUser);
    }

    @Test
    public void testOnboardUser_AlreadyOnboarded() {
        // given: a user that has already been onboarded
        User user = mockUser(UUID.randomUUID()).toBuilder()
            .dateOnboarded(Instant.now())
            .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // when: onboarding the user - an exception is thrown
        assertThrows(UserAlreadyOnboardedException.class, () ->
            fixture.onboardUser(user.getId())
        );

        // then: the user is NOT updated
        verify(userRepository, never()).save(any());

        // and: NO event is sent
        verify(userEventSender, never()).sendUserOnboarded(any());
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
