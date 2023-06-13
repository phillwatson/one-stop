package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.commons.Strings;
import com.hillayes.events.events.auth.SuspiciousActivity;
import com.hillayes.exception.common.MissingParameterException;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.MagicToken;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.errors.UserRegistrationException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.MagicTokenRepository;
import com.hillayes.user.repository.UserRepository;
import com.hillayes.user.resource.UserOnboardResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final DeletedUserRepository deletedUserRepository;
    private final MagicTokenRepository magicTokenRepository;
    private final PasswordCrypto passwordCrypto;
    private final UserEventSender userEventSender;
    private final Gateway gateway;

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

        MagicToken token = magicTokenRepository.save(MagicToken.builder()
            .email(email.toLowerCase())
            .token(UUID.randomUUID().toString())
            .expires(Instant.now().plus(5, ChronoUnit.MINUTES))
            .build());

        try {
            URI acknowledgerUri = UriBuilder
                .fromResource(UserOnboardResource.class)
                .path(UserOnboardResource.class, "acknowledgeUser")
                .scheme(gateway.getScheme())
                .host(gateway.getHost())
                .port(gateway.getPort())
                .buildFromMap(Map.of("token", token.getToken()));
            userEventSender.sendUserRegistered(token, acknowledgerUri);

            log.debug("User registered [email: {}, ackUri: {}]", email, acknowledgerUri);
        } catch (IllegalArgumentException e) {
            log.error("Failed to construct acknowledge URI [email: {}]", email, e);
            throw new UserRegistrationException(email);
        } catch (IOException e) {
            log.warn("Failed to register email [email: {}]", email, e);
            throw new UserRegistrationException(email);
        }
    }

    public Optional<User> acknowledgeToken(String token) {
        log.info("User has acknowledged onboarding token [token: {}]", token);

        return magicTokenRepository.findByToken(token)
            .filter(t -> t.getExpires().isAfter(Instant.now()))
            .map(t -> {
                // create a user record - but not yet onboarded
                User newUser = User.builder()
                    .username(t.getEmail())
                    .email(t.getEmail())
                    .givenName(t.getEmail())
                    .passwordHash(passwordCrypto.getHash(UUID.randomUUID().toString().toCharArray()))
                    .roles(Set.of("onboarding")) // allow user to complete onboarding but nothing else
                    .build();

                newUser = userRepository.save(newUser);
                magicTokenRepository.delete(t);

                userEventSender.sendUserAcknowledged(newUser);
                log.debug("Created user [username: {}, id: {}]", newUser.getUsername(), newUser.getId());
                return newUser;
            });
    }

    public Optional<User> completeOnboarding(UUID id, User modifiedUser, char[] password) {
        log.info("User has completed onboarding [userId: {}]", id);

        // ensure mandatory fields are present
        validateUserContent(modifiedUser);

        // ensure username is unique
        userRepository.findByUsername(modifiedUser.getUsername())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(existing.getUsername());
            });

        // ensure email is unique
        validateUniqueEmail(id, modifiedUser.getEmail());

        return userRepository.findById(id)
            .map(user -> {
                // update and onboard user
                user = userRepository.save(user.toBuilder()
                    .username(modifiedUser.getUsername())
                    .email(modifiedUser.getEmail())
                    .title(modifiedUser.getTitle())
                    .givenName(modifiedUser.getGivenName())
                    .familyName(modifiedUser.getFamilyName())
                    .preferredName(modifiedUser.getPreferredName())
                    .phoneNumber(modifiedUser.getPhoneNumber())
                    .passwordHash(passwordCrypto.getHash(password))
                    .dateOnboarded(Instant.now())
                    .roles(Set.of("user"))
                    .build());

                userEventSender.sendUserCreated(user);
                log.debug("User has completed onboarding [id: {}, username: {}]", user.getId(), user.getUsername());
                return user;
            });
    }

    public Optional<User> getUser(UUID id) {
        log.info("Retrieving user [userId: {}]", id);
        Optional<User> result = userRepository.findById(id);

        log.debug("Retrieved user [userId: {}, found: {}]", id, result.isPresent());
        return result;
    }

    public Page<User> listUsers(int page, int pageSize) {
        log.info("Listing user [page: {}, pageSize: {}]", page, pageSize);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by("username").ascending());
        Page<User> result = userRepository.findAll(pageRequest);
        log.debug("Listing users [page: {}, pageSize: {}, size: {}]",
            page, pageSize, result.getNumberOfElements());

        return result;
    }

    public Optional<User> updatePassword(UUID userId,
                                         char[] oldPassword,
                                         char[] newPassword) {
        log.info("Updating password [userId: {}]", userId);

        return userRepository.findById(userId)
            .filter(user -> passwordCrypto.verify(oldPassword, user.getPasswordHash()))
            .map(user -> {
                user = userRepository.save(user.toBuilder()
                    .passwordHash(passwordCrypto.getHash(newPassword))
                    .build());

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

        return userRepository.findById(id)
            .map(user -> {
                User.UserBuilder userBuilder = user.toBuilder()
                    .username(userRequest.getUsername())
                    .email(userRequest.getEmail())
                    .title(userRequest.getTitle())
                    .givenName(userRequest.getGivenName())
                    .familyName(userRequest.getFamilyName())
                    .preferredName(userRequest.getPreferredName())
                    .phoneNumber(userRequest.getPhoneNumber());

                if ((userRequest.getRoles() != null) && (!userRequest.getRoles().isEmpty())) {
                    userBuilder.roles(userRequest.getRoles());
                }

                return userRepository.save(userBuilder.build());
            }).map(user -> {
                userEventSender.sendUserUpdated(user);
                log.debug("Updated user [username: {}, userId: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    public Optional<User> deleteUser(UUID id) {
        log.info("Deleting user [userId: {}]", id);

        return userRepository.findById(id)
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
        // delete all expired onboarding tokens
        magicTokenRepository.deleteByExpiresBefore(Instant.now());

        // ensure no onboarding user has the same email
        magicTokenRepository.findByEmail(email.toLowerCase())
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(email);
            });

        // ensure no onboarded user has the same email
        findUserByEmail(email.toLowerCase())
            .filter(existing -> (userId == null) || (!existing.getId().equals(userId)))
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(email);
            });
    }
}
