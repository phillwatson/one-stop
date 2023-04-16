package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.UserRepository;
import org.springframework.data.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final DeletedUserRepository deletedUserRepository;
    private final PasswordCrypto passwordCrypto;
    private final UserEventSender userEventSender;

    public User createUser(String username, char[] password, User user) {
        log.info("Creating user [username: {}]", username);

        // includes deleted users in duplicate username comparison
        userRepository.findByUsername(username)
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(username);
            });

        // excludes deleted users in duplicate email comparison
        String email = user.getEmail();
        userRepository.findByEmail(email)
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(email);
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
                if (user.isOnboarded()) {
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

    public Optional<User> updateUser(UUID id, User modifiedUser) {
        log.info("Updating user [userId: {}]", id);

        return userRepository.findById(id)
            .map(user -> {
                user.setEmail(modifiedUser.getEmail());
                user.setTitle(modifiedUser.getTitle());
                user.setGivenName(modifiedUser.getGivenName());
                user.setFamilyName(modifiedUser.getFamilyName());
                user.setPreferredName(modifiedUser.getPreferredName());
                user.setPhoneNumber(modifiedUser.getPhoneNumber());
                user = userRepository.save(user);

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
}
