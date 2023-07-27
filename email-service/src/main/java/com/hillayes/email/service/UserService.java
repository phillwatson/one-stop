package com.hillayes.email.service;

import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SendEmailService sendEmailService;

    public void createUser(User user) {
        log.info("Created user [id: {}, username: {}]", user.getId(), user.getUsername());
        User existingUser = userRepository.findById(user.getId())
            .orElse(null);

        // if user already exists
        if (existingUser != null) {
            if (existingUser.getDateCreated() != null) {
                // must be a duplicate event
                return;
            }

            // out-of-order update must have been received before create
            // record the creation date and save
            existingUser.setDateCreated(user.getDateCreated());
            existingUser = userRepository.save(existingUser);
        } else {
            // save new record
            existingUser = userRepository.save(user);
        }

        // send email to notify user of creation
        Map<String, Object> params = Map.of("user", existingUser);
        sendEmailService.sendEmail(TemplateName.USER_CREATED, new SendEmailService.Recipient(existingUser), params);
    }

    public void onboardUser(UUID userId) {
        log.info("Onboarded user [id: {}]", userId);
        userRepository.findById(userId).ifPresent(user -> {
            // send email to notify user of onboarding
            log.debug("Sending email to notify user onboarding [email: {}]", user.getEmail());
        });
    }

    public Optional<User> getUser(UUID userId) {
        log.info("Get user [id: {}]", userId);
        Optional<User> result = userRepository.findById(userId);
        if (result.isEmpty()) {
            log.warn("User not found [id: {}]", userId);
        }
        return result;
    }

    public User updateUser(User user) {
        log.info("Updated user [id: {}, username: {}]", user.getId(), user.getUsername());
        User existingUser = userRepository.findById(user.getId())
            .orElse(null);

        if (existingUser != null) {
            // if the user has already been updated
            // and update date more recent than this
            if ((existingUser.getDateUpdated() != null) &&
                (existingUser.getDateUpdated().compareTo(user.getDateUpdated()) >= 0)) {
                // duplicate or out-of-order event
                return existingUser;
            }
        } else {
            // out-of-order - user hasn't been created yet
            // assume given user is 
            existingUser = user;
        }

        // record old recipient info so we can email the user
        SendEmailService.Recipient oldRecipient = new SendEmailService.Recipient(existingUser);

        // updated and save the user
        existingUser.setDateUpdated(user.getDateUpdated());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setTitle(user.getTitle());
        existingUser.setGivenName(user.getGivenName());
        existingUser.setFamilyName(user.getFamilyName());
        existingUser.setPreferredName(user.getPreferredName());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setLocale(user.getLocale());
        existingUser = userRepository.save(existingUser);

        // send email to notify user of change
        log.debug("Sending email to notify user update [oldEmail: {}, newEmail: {}]",
            oldRecipient.email(), user.getEmail());

        Map<String, Object> params = Map.of("user", existingUser);

        // send email to notify user of update to their account
        if (! oldRecipient.email().equalsIgnoreCase(existingUser.getEmail())) {
            sendEmailService.sendEmail(TemplateName.USER_UPDATED, oldRecipient, params);
        }
        sendEmailService.sendEmail(TemplateName.USER_UPDATED, new SendEmailService.Recipient(user), params);

        return existingUser;
    }

    public void deleteUser(UUID userId) {
        log.info("Deleted user [id: {}]", userId);
        userRepository.findById(userId).ifPresent(user -> {
            userRepository.delete(user);

            // send email to notify user of deletion
            Map<String, Object> params = Map.of("user", user);
            sendEmailService.sendEmail(TemplateName.USER_DELETED, new SendEmailService.Recipient(user), params);
        });
    }
}
