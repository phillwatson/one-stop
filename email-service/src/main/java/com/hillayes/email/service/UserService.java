package com.hillayes.email.service;

import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public User createUser(User user) {
        log.info("Created user [id: {}, username: {}]", user.getId(), user.getUsername());
        User result = userRepository.save(user);

        // send email to notify user of creation
        log.debug("Sending email to notify user creation [email: {}]", user.getEmail());

        return result;
    }

    public void onboardUser(UUID userId) {
        log.info("Onboarded user [id: {}]", userId);
        userRepository.findById(userId).ifPresent(user -> {
            // send email to notify user of onboarding
            log.debug("Sending email to notify user onboarding [email: {}]", user.getEmail());
        });
    }

    public void deleteUser(UUID userId) {
        log.info("Deleted user [id: {}]", userId);
        userRepository.findById(userId).ifPresent(user -> {
            userRepository.delete(user);

            // send email to notify user of deletion
            log.debug("Sending email to notify user deletion [email: {}]", user.getEmail());
        });
    }

    public User updateUser(User user) {
        log.info("Updated user [id: {}, username: {}]", user.getId(), user.getUsername());
        User update = userRepository.findById(user.getId())
            .orElse(user); // create a new record

        String oldEmail = update.getEmail();

        update.setUsername(user.getUsername());
        update.setEmail(user.getEmail());
        update.setTitle(user.getTitle());
        update.setGivenName(user.getGivenName());
        update.setFamilyName(user.getFamilyName());
        update.setPreferredName(user.getPreferredName());
        update = userRepository.save(update);

        // send email to notify user of change
        log.debug("Sending email to notify user update [oldEmail: {}, newEmail: {}]", oldEmail, user.getEmail());

        return update;
    }
}
