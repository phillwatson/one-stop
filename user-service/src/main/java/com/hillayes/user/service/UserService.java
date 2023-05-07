package com.hillayes.user.service;

import com.hillayes.auth.crypto.PasswordCrypto;
import com.hillayes.user.domain.DeletedUser;
import com.hillayes.user.domain.MagicToken;
import com.hillayes.user.domain.User;
import com.hillayes.user.errors.DuplicateEmailAddressException;
import com.hillayes.user.errors.DuplicateUsernameException;
import com.hillayes.user.event.UserEventSender;
import com.hillayes.user.repository.DeletedUserRepository;
import com.hillayes.user.repository.MagicTokenRepository;
import com.hillayes.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Instant;
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

    public void registerUser(String email) {
        log.info("Registering user [email: {}]", email);

        // ensure email is unique
        userRepository.findByEmail(email)
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(email);
            });

        MagicToken token = magicTokenRepository.save(MagicToken.builder()
            .email(email)
            .token(UUID.randomUUID().toString())
            .expires(Instant.now().plusSeconds(1000 * 60 * 5))
            .build());

        userEventSender.sendUserRegistered(token);
    }

    public Optional<User> acknowledgeToken(String token) {
        log.info("User has acknowledged onboarding token [token: {}]", token);

        return magicTokenRepository.findByToken(token)
            .filter(t -> t.getExpires().isAfter(Instant.now()))
            .map(t -> {
                User newUser = User.builder()
                    .username(t.getEmail())
                    .email(t.getEmail())
                    .givenName(t.getEmail())
                    .passwordHash(passwordCrypto.getHash(UUID.randomUUID().toString().toCharArray()))
                    .roles(Set.of("user"))
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

        // ensure username is unique
        userRepository.findByUsername(modifiedUser.getUsername())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(existing.getUsername());
            });

        // ensure email is unique
        userRepository.findByEmail(modifiedUser.getEmail())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(existing.getEmail());
            });

        return userRepository.findById(id)
            .map(user -> {
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

    public Optional<User> updateUser(UUID id, User modifiedUser) {
        log.info("Updating user [userId: {}]", id);

        // ensure username is unique
        userRepository.findByUsername(modifiedUser.getUsername())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateUsernameException(existing.getUsername());
            });

        // ensure email is unique
        userRepository.findByEmail(modifiedUser.getEmail())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new DuplicateEmailAddressException(existing.getEmail());
            });

        return userRepository.findById(id)
            .map(user -> {
                user = userRepository.save(user.toBuilder()
                    .username(modifiedUser.getUsername())
                    .email(modifiedUser.getEmail())
                    .title(modifiedUser.getTitle())
                    .givenName(modifiedUser.getGivenName())
                    .familyName(modifiedUser.getFamilyName())
                    .preferredName(modifiedUser.getPreferredName())
                    .phoneNumber(modifiedUser.getPhoneNumber())
                    .build());

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
