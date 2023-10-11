package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.auth.jwt.AuthTokens;
import com.hillayes.commons.Strings;
import com.hillayes.commons.jpa.Page;
import com.hillayes.commons.net.Gateway;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.OidcIdentity;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.errors.UserRegistrationException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.UserRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Transactional
@Slf4j
public class UserService {
    @Inject
    UserRepository userRepository;
    @Inject
    DeletedUserRepository deletedUserRepository;
    @Inject
    PasswordCrypto passwordCrypto;
    @Inject
    UserEventSender userEventSender;
    @Inject
    Gateway gateway;
    @Inject
    AuthTokens authTokens;
    @ConfigProperty(name = "one-stop.auth.onboarding.token-expires-in")
    Duration tokenDuration;


    /**
     * Sends an email to a client that wishes to register as a user. The email
     * will contain a time-limited token (in the form of a URL link). That will
     * direct the user to the App page to enter their user details before calling
     * the completeOnboarding() below.
     *
     * @param email the email of the client wishing to register.
     */
    public void registerUser(String email) {
        log.info("Registering user [email: {}]", email);

        try {
            // ensure email is unique
            validateUniqueEmail(null, email);
        } catch (DuplicateEmailAddressException e) {
            findUserByEmail(email).ifPresent(user ->
                userEventSender.sendAccountActivity(user, SuspiciousActivity.EMAIL_REGISTRATION)
            );
            return;
        }

        try {
            String token = authTokens.generateToken(email.toLowerCase(), tokenDuration);

            URI acknowledgerUri = UriBuilder.newInstance()
                .scheme(gateway.getScheme())
                .host(gateway.getHost())
                .port(gateway.getPort())
                .path("/onboard-user")
                .queryParam("token", token)
                .build();
            userEventSender.sendUserRegistered(email, tokenDuration, acknowledgerUri);

            log.debug("User registered [email: {}, ackUri: {}]", email, acknowledgerUri);
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to construct acknowledge URI [email: {}]", email, e);
            throw new UserRegistrationException(email);
        }
    }

    /**
     * Called when a client wishing to register has completed the required user
     * profile information. It will check that the profile information is valid
     * before creating a new user.
     *
     * @param userProfile the profile data for the new user.
     * @param password the password selected by the new user.
     * @return the onboarded user.
     */
    public User completeOnboarding(User userProfile, char[] password) {
        log.info("User has completed onboarding [username: {}]", userProfile.getUsername());

        // ensure mandatory fields are present
        validateUserContent(userProfile);

        // ensure username is unique
        userRepository.findByUsername(userProfile.getUsername())
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(existing.getUsername());
            });

        // ensure email is unique
        validateUniqueEmail(null, userProfile.getEmail());

        userProfile.setUsername(userProfile.getUsername());
        userProfile.setEmail(userProfile.getEmail());
        userProfile.setTitle(userProfile.getTitle());
        userProfile.setGivenName(userProfile.getGivenName());
        userProfile.setFamilyName(userProfile.getFamilyName());
        userProfile.setPreferredName(userProfile.getPreferredName());
        userProfile.setPhoneNumber(userProfile.getPhoneNumber());
        userProfile.setLocale(userProfile.getLocale());
        userProfile.setPasswordHash(passwordCrypto.getHash(password));
        userProfile.setDateOnboarded(Instant.now());
        userProfile.setRoles(Set.of("user"));
        userProfile = userRepository.save(userProfile);

        userEventSender.sendUserCreated(userProfile);
        log.debug("User has completed onboarding [id: {}, username: {}]", userProfile.getId(), userProfile.getUsername());
        return userProfile;
    }

    public Optional<User> getUser(UUID id) {
        log.info("Retrieving user [userId: {}]", id);
        Optional<User> result = userRepository.findByIdOptional(id);

        log.debug("Retrieved user [userId: {}, found: {}]", id, result.isPresent());
        return result;
    }

    public Page<User> listUsers(int page, int pageSize) {
        log.info("Listing user [page: {}, pageSize: {}]", page, pageSize);

        Page<User> result = userRepository.findAll(Sort.by("username"), page, pageSize);
        log.debug("Listing users [page: {}, pageSize: {}, size: {}]",
            page, pageSize, result.getContentSize());

        return result;
    }

    public Collection<OidcIdentity> getUserAuthProviders(UUID id) {
        log.info("Retrieving user auth-providers [userId: {}]", id);
        Optional<User> user = userRepository.findByIdOptional(id);
        Set<OidcIdentity> oidcIdentities = user.map(User::getOidcIdentities).orElse(Set.of());

        // the call to size() will load the lazy collection
        log.debug("Retrieved user auth-providers [userId: {}, size: {}]", id, oidcIdentities.size());
        return oidcIdentities;
    }

    public Optional<User> updatePassword(UUID userId,
                                         char[] oldPassword,
                                         char[] newPassword) {
        log.info("Updating password [userId: {}]", userId);

        if ((oldPassword == null) || (oldPassword.length == 0)) {
            throw new MissingParameterException("oldPassword");
        }

        if ((newPassword == null) || (newPassword.length == 0)) {
            throw new MissingParameterException("newPassword");
        }

        return userRepository.findByIdOptional(userId)
            .filter(user -> passwordCrypto.verify(oldPassword, user.getPasswordHash()))
            .map(user -> {
                user.setPasswordHash(passwordCrypto.getHash(newPassword));
                user = userRepository.save(user);

                userEventSender.sendUserUpdated(user);
                log.debug("Updated password [username: {}, userId: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    public Optional<User> updateUser(UUID id, User userRequest) {
        log.info("Updating user [userId: {}]", id);

        // ensure mandatory fields are present
        validateUserContent(userRequest);

        // ensure username is unique
        userRepository.findByUsername(userRequest.getUsername())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(existing.getUsername());
            });

        // ensure email is unique
        validateUniqueEmail(id, userRequest.getEmail());

        return userRepository.findByIdOptional(id)
            .map(user -> {
                user.setUsername(userRequest.getUsername());
                user.setEmail(userRequest.getEmail());
                user.setTitle(userRequest.getTitle());
                user.setGivenName(userRequest.getGivenName());
                user.setFamilyName(userRequest.getFamilyName());
                user.setPreferredName(userRequest.getPreferredName());
                user.setPhoneNumber(userRequest.getPhoneNumber());
                user.setLocale(userRequest.getLocale());
                if ((userRequest.getRoles() != null) && (!userRequest.getRoles().isEmpty())) {
                    user.setRoles(userRequest.getRoles());
                }

                return userRepository.save(user);
            }).map(user -> {
                userEventSender.sendUserUpdated(user);
                log.debug("Updated user [username: {}, userId: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    public Optional<User> deleteUser(UUID id) {
        log.info("Deleting user [userId: {}]", id);

        return userRepository.findByIdOptional(id)
            .map(user -> {
                DeletedUser deletedUser = deletedUserRepository.save(DeletedUser.fromUser(user));
                userRepository.delete(user);

                userEventSender.sendUserDeleted(deletedUser);
                log.debug("Deleted user [username: {}, id: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    /**
     * Validates the content of the given user instance for mandatory fields.
     */
    private void validateUserContent(User user) {
        if (Strings.isBlank(user.getUsername())) {
            throw new MissingParameterException("username");
        }

        if (Strings.isBlank(user.getEmail())) {
            throw new MissingParameterException("email");
        }

        if (Strings.isBlank(user.getGivenName())) {
            throw new MissingParameterException("givenName");
        }
    }

    private Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    /**
     * Looks for any other user (onboarding or onboarded) with the same email address.
     *
     * @param userId the ID of the user being updated (or null if new)
     * @param email the email address to check
     */
    private void validateUniqueEmail(UUID userId, String email) {
        // ensure no onboarded user has the same email
        findUserByEmail(email.toLowerCase())
            .filter(existing -> !existing.getId().equals(userId))
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(email);
            });
    }
}
