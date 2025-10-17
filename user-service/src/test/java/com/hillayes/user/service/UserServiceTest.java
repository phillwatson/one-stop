package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.commons.jpa.OrderBy;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.net.Gateway;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.openid.AuthProvider;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.errors.UserRegistrationException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.hillayes.user.utils.TestData.mockUser;
import static com.hillayes.user.utils.TestData.mockUsers;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private final UserRepository userRepository = mock();
    private final DeletedUserRepository deletedUserRepository = mock();
    private final PasswordCrypto passwordCrypto = mock();
    private final AuthTokens authTokens = mock();
    private final UserEventSender userEventSender = mock();
    private final Gateway gateway = mock();

    private final UserService fixture = new UserService(
        userRepository,
        deletedUserRepository,
        passwordCrypto,
        userEventSender,
        gateway,
        authTokens,
        Duration.ofMinutes(30)
    );

    @BeforeEach
    public void beforeEach() {
        // simulate a successful password hash
        when(passwordCrypto.getHash(any())).thenReturn(insecure().nextAlphanumeric(20));

        // simulate the user repository returns the user with an ID
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
    }

    @Test
    public void testRegisterUser() {
        // given: an email address
        String email = insecure().nextAlphanumeric(10) + "@example.com";

        // and: a token is generated
        String token = insecure().nextAlphanumeric(30);
        when(authTokens.generateToken(eq(email.toLowerCase()), any())).thenReturn(token);

        // when: we register a user
        fixture.registerUser(email);

        // then: a token is generated
        verify(authTokens).generateToken(eq(email.toLowerCase()), any());

        // and: a registration is recorded
        ArgumentCaptor<URI> tokenCaptor = ArgumentCaptor.forClass(URI.class);
        verify(userEventSender).sendUserRegistered(any(), any(), tokenCaptor.capture());

        // and: the magic-token link contains the token
        URI tokenLink = tokenCaptor.getValue();
        assertTrue(tokenLink.getQuery().contains("token=" + token));
    }

    @Test
    public void testRegisterUser_DuplicateEmail() {
        // given: an email address
        String email = insecure().nextAlphanumeric(10) + "@example.com";

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
    public void testRegisterUser_GatewayError() throws IOException {
        // given: an email address
        String email = insecure().nextAlphanumeric(10) + "@example.com";

        // and: a token is generated
        String token = insecure().nextAlphanumeric(30);
        when(authTokens.generateToken(eq(email.toLowerCase()), any())).thenReturn(token);

        // and: a gateway error occurs
        when(gateway.getHost()).thenThrow(new IOException());

        // when: we register a user
        // then: an exception is raised
        UserRegistrationException exception =
            assertThrows(UserRegistrationException.class, () -> fixture.registerUser(email));

        // and: a registration is recorded
        assertEquals(email, exception.getParameter("email"));
    }

    @Test
    public void testCompleteOnboarding() {
        // given: a completed user request
        UUID userId = UUID.randomUUID();
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
        User user = mockUser();

        // and: the user was previously registered and acknowledged
        User registeredUser = mockUser(UUID.randomUUID());
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.of(registeredUser));

        Instant timeServiceCalled = Instant.now();

        // when: completing the onboarding
        User onboardedUser = fixture.completeOnboarding(user, password);

        // then: the user is returned
        assertNotNull(onboardedUser);

        // and: the user is onboarded
        assertTrue(onboardedUser.isOnboarded());

        // and: the user's passwordLastSet date is recorded
        assertTrue(onboardedUser.getPasswordLastSet().isAfter(timeServiceCalled));

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
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
        User user = mockUser();
        user.setUsername(null);

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
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
        User user = mockUser();
        user.setEmail(null);

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
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
        User user = mockUser();
        user.setGivenName(null);

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
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
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
        char[] password = insecure().nextAlphanumeric(20).toCharArray();
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

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
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

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
        Page<User> page = Page.of(users);
        when(userRepository.pageAll(any(OrderBy.class), anyInt(), anyInt())).thenReturn(page);

        // when: the service is called
        Page<User> result = fixture.listUsers(1, 20);

        // then: the users are returned
        assertEquals(users.size(), result.getContentSize());
    }

    @Test
    public void testGetUserAuthProviders() {
        // given: a user with some Open-ID identifiers
        User user = mockUser(UUID.randomUUID());
        for (AuthProvider authProvider : AuthProvider.values()) {
            user.addOidcIdentity(authProvider, insecure().nextAlphanumeric(20), insecure().nextAlphanumeric(15));
        }
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // when: the service is called
        Collection<OidcIdentity> result = fixture.getUserAuthProviders(user.getId());

        // then: the user's oidc identifiers are returned
        assertEquals(user.getOidcIdentities().size(), result.size());
        user.getOidcIdentities().forEach(expected ->
            assertNotNull(result.stream()
                .filter(identifier -> identifier.getProvider() == expected.getProvider())
                .findFirst().orElse(null))
        );
    }

    @Test
    public void testGetUserAuthProviders_UserNotFound() {
        // given: no user with the given ID
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

        // when: the service is called
        Collection<OidcIdentity> result = fixture.getUserAuthProviders(userId);

        // then: the user's oidc identifiers are empty
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdatePassword() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        user.setPasswordHash("oldhash");
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: the given old password is correct
        when(passwordCrypto.verify("oldpassword".toCharArray(), user.getPasswordHash())).thenReturn(true);

        // and: the user's passwordLastSet date is recorded
        Instant passwordLastSet = user.getPasswordLastSet();

        // when: the service is called
        Optional<User> result = fixture.updatePassword(user.getId(), "oldpassword".toCharArray(), "newpassword".toCharArray());

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the passwordLastSet date is updated
        assertNotEquals(passwordLastSet, result.get().getPasswordLastSet());

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
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

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
        User user = mockUser(UUID.randomUUID());
        user.setPasswordHash("oldhash");
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

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
    public void testUpdatePassword_MissingOldPassword() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        user.setPasswordHash("oldhash");
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a new password
        char[] newPassword = "password".toCharArray();

        // when: the service is called without an old password
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updatePassword(user.getId(), null, newPassword)
        );

        // then: the user is NOT returned
        assertEquals("oldPassword", exception.getParameter("parameter-name"));

        // and: the password is NOT hashed
        verify(passwordCrypto, never()).getHash(any());

        // and: the user is NOT saved
        verify(userRepository, never()).save(any());

        // and: an event is NOT sent
        verify(userEventSender, never()).sendUserUpdated(any());
    }

    @Test
    public void testUpdatePassword_MissingNewPassword() {
        // given: a user
        User user = mockUser(UUID.randomUUID());
        user.setPasswordHash("oldhash");
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: an old password
        char[] oldPassword = "password".toCharArray();

        // when: the service is called without a new password
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updatePassword(user.getId(), oldPassword, null)
        );

        // then: the user is NOT returned
        assertEquals("newPassword", exception.getParameter("parameter-name"));

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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: no other user with the given username exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(user));

        // and: no other user with the given email exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(user));

        // when: the service is called
        user.setGivenName("New First Name");
        user.setFamilyName("New Last Name");
        Optional<User> result = fixture.updateUser(user.getId(), user);

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the user is updated
        assertEquals(user.getId(), result.get().getId());
        assertEquals(user.getGivenName(), result.get().getGivenName());
        assertEquals(user.getFamilyName(), result.get().getFamilyName());

        // and: the user is saved
        verify(userRepository).save(any());

        // and: an event is sent
        verify(userEventSender).sendUserUpdated(result.get());
    }

    @Test
    public void testUpdateUser_NotFound() {
        // given: an unknown user
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        user.setUsername(null);
        user.setGivenName("New First Name");
        user.setFamilyName("New Last Name");
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), user)
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        user.setEmail(null);
        user.setGivenName("New First Name");
        user.setFamilyName("New Last Name");
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), user)
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        user.setGivenName(null);
        user.setFamilyName("New Last Name");
        MissingParameterException exception = assertThrows(MissingParameterException.class, () ->
            fixture.updateUser(user.getId(), user)
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a user with the given username already exists
        when(userRepository.findByUsername(user.getUsername()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        user.setGivenName("New First Name");
        user.setFamilyName("New Last Name");
        assertThrows(DuplicateUsernameException.class, () ->
            fixture.updateUser(user.getId(), user)
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // and: a user with the given email already exists
        when(userRepository.findByEmail(user.getEmail().toLowerCase()))
            .thenReturn(Optional.of(mockUser(UUID.randomUUID())));

        // when: updating a user - an exception is thrown
        user.setGivenName("New First Name");
        user.setFamilyName("New Last Name");
        assertThrows(DuplicateEmailAddressException.class, () ->
            fixture.updateUser(user.getId(), user)
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
        when(userRepository.findByIdOptional(user.getId())).thenReturn(Optional.of(user));

        // when: the service is called
        Optional<User> result = fixture.deleteUser(user.getId());

        // then: the user is returned
        assertTrue(result.isPresent());

        // and: the user is deleted
        verify(userRepository).delete(user);

        // and: the deleted user is saved to the deleted user repository
        ArgumentCaptor<DeletedUser> captor = ArgumentCaptor.forClass(DeletedUser.class);
        verify(deletedUserRepository).save(captor.capture());

        // and: the deleted user has the same values as the user
        DeletedUser deletedUser = captor.getValue();
        assertNotNull(deletedUser.getDateDeleted());
        assertEquals(user.getId(), deletedUser.getId());
        assertEquals(user.getUsername(), deletedUser.getUsername());
        assertEquals(user.getPasswordHash(), deletedUser.getPasswordHash());
        assertEquals(user.getEmail(), deletedUser.getEmail());
        assertEquals(user.getTitle(), deletedUser.getTitle());
        assertEquals(user.getGivenName(), deletedUser.getGivenName());
        assertEquals(user.getFamilyName(), deletedUser.getFamilyName());
        assertEquals(user.getPreferredName(), deletedUser.getPreferredName());
        assertEquals(user.getPhoneNumber(), deletedUser.getPhoneNumber());
        assertEquals(user.getLocale(), deletedUser.getLocale());
        assertEquals(user.getDateCreated(), deletedUser.getDateCreated());
        assertEquals(user.getDateOnboarded(), deletedUser.getDateOnboarded());
        assertEquals(user.getDateBlocked(), deletedUser.getDateBlocked());
        assertEquals(user.getBlockedReason(), deletedUser.getBlockedReason());
        assertEquals(user.getVersion(), deletedUser.getVersion());

        // and: an event is sent
        verify(userEventSender).sendUserDeleted(any());
    }

    @Test
    public void testDeleteUser_NotFound() {
        // given: an unknown user
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

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
