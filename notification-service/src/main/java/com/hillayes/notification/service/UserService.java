package com.hillayes.notification.service;

import com.hillayes.commons.Strings;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.repository.UserRepository;
import com.hillayes.notification.task.SendEmailTask;
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
    private final SendEmailTask sendEmailTask;

    public void createUser(User user) {
        log.info("Created user [id: {}, username: {}]", user.getId(), user.getUsername());
        User existingUser = userRepository.findByIdOptional(user.getId())
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
        sendEmailTask.queueJob(new SendEmailService.Recipient(existingUser), TemplateName.USER_CREATED, params);
    }

    public Optional<User> getUser(UUID userId) {
        log.trace("Get user [id: {}]", userId);
        Optional<User> result = userRepository.findByIdOptional(userId);
        if (result.isEmpty()) {
            log.warn("User not found [id: {}]", userId);
        }
        return result;
    }

    public User updateUser(User user) {
        log.info("Updated user [id: {}, username: {}]", user.getId(), user.getUsername());
        User existingUser = userRepository.findByIdOptional(user.getId())
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
            Strings.maskEmail(oldRecipient.getEmail()), Strings.maskEmail(user.getEmail()));

        Map<String, Object> params = Map.of("user", existingUser);

        // send email to notify user of update to their account
        if (! oldRecipient.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            sendEmailTask.queueJob(oldRecipient, TemplateName.USER_UPDATED, params);
        }
        sendEmailTask.queueJob(new SendEmailService.Recipient(user), TemplateName.USER_UPDATED, params);

        return existingUser;
    }

    public void deleteUser(UUID userId) {
        log.info("Deleted user [id: {}]", userId);
        userRepository.findByIdOptional(userId).ifPresent(user -> {
            userRepository.delete(user);

            // send email to notify user of deletion
            Map<String, Object> params = Map.of("user", user);
            sendEmailTask.queueJob(new SendEmailService.Recipient(user), TemplateName.USER_DELETED, params);
        });
    }
}
