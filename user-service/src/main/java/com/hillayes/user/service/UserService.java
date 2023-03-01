package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.events.UserEventSender;
import com.hillayes.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;

import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.*;

@Singleton
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordCrypto passwordCrypto;
    private final UserEventSender userEventSender;

    public User createUser(String username, char[] password, User user) {
        log.info("Creating user [username: {}]", username);

        userRepository.findByUsername(username)
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(username);
            });

        user = userRepository.save(user.toBuilder()
            .username(username)
            .passwordHash(passwordCrypto.getHash(password))
            .roles(Set.of("user"))
            .build());

        userEventSender.sendUserCreated(user);

        log.debug("Created user [username: {}, id: {}]", user.getUsername(), user.getId());
        return user;
    }

    public Optional<User> onboardUser(UUID id) {
        log.info("Onboard user [userId: {}]", id);

        return userRepository.findById(id)
            .map(user -> {
                if (user.getDateOnboarded() != null) {
                    throw new BadRequestException("User is already onboard");
                }

                user.setDateOnboarded(Instant.now());
                User result = userRepository.save(user);

                userEventSender.sendUserOnboarded(result);
                log.debug("Onboarded user [username: {}, userId: {}]", result.getUsername(), result.getId());
                return result;
            });
    }

    public Optional<User> getUser(UUID id) {
        log.info("Retrieving user [userId: {}]", id);
        Optional<User> result = userRepository.findById(id);

        log.debug("Retrieved user [userId: {}, found: {}]", id, result.isPresent());
        return result;
    }

    public Collection<User> listUsers() {
        log.info("Retrieving users");
        Collection<User> result = userRepository.findAll(Sort.by("username").ascending());

        log.debug("Retrieved users [size: {}]", result.size());
        return result;
    }

    public Optional<User> updateUser(UUID id, User modifiedUser) {
        log.info("Updating user [userId: {}]", id);

        return userRepository.findById(id)
            .map(user -> {
                user.setEmail(modifiedUser.getEmail());
                user.setGivenName(modifiedUser.getGivenName());
                user.setFamilyName(modifiedUser.getFamilyName());
                user.setPhoneNumber(modifiedUser.getPhoneNumber());
                userRepository.save(user);

                userEventSender.sendUserUpdated(user);
                log.debug("Updated user [username: {}, userId: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    public Optional<User> deleteUser(UUID id) {
        log.info("Deleting user [userId: {}]", id);

        return userRepository.findById(id)
            .map(user -> {
                userRepository.delete(user);

                userEventSender.sendUserDeleted(user);
                log.debug("Deleted user [username: {}, id: {}]", user.getUsername(), user.getId());
                return user;
            });
    }

    public Collection<String> getUserRoles(UUID userId) {
        log.info("Retrieving user roles [userId: {}]", userId);
        Collection<String> result = userRepository.findById(userId)
            .map(User::getRoles)
            .orElse(Collections.emptySet());

        log.debug("Retrieved user roles [userId: {}, size: {}]", userId, result.size());
        return result;
    }
}
