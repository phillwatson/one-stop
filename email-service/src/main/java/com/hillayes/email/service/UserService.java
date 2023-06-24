package com.hillayes.email.service;

import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
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
        user = userRepository.save(user);

        // send email to notify user of creation
        sendEmailService.sendEmail(TemplateName.USER_CREATED, new SendEmailService.Recipient(user));
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
            sendEmailService.sendEmail(TemplateName.USER_DELETED, new SendEmailService.Recipient(user));
        });
    }

    public User updateUser(User user) {
        log.info("Updated user [id: {}, username: {}]", user.getId(), user.getUsername());
        User update = userRepository.findById(user.getId())
            .orElse(user); // create a new record

        // record old recipient info so we can email the user
        SendEmailService.Recipient oldRecipient = new SendEmailService.Recipient(update);

        update.setUsername(user.getUsername());
        update.setEmail(user.getEmail());
        update.setTitle(user.getTitle());
        update.setGivenName(user.getGivenName());
        update.setFamilyName(user.getFamilyName());
        update.setPreferredName(user.getPreferredName());
        update.setLocale(user.getLocale());
        update = userRepository.save(update);

        // send email to notify user of change
        log.debug("Sending email to notify user update [oldEmail: {}, newEmail: {}]",
            oldRecipient.email(), user.getEmail());

        // send email to notify user of update to their account
        if (! oldRecipient.email().equalsIgnoreCase(update.getEmail())) {
            sendEmailService.sendEmail(TemplateName.USER_UPDATED, oldRecipient);
        }
        sendEmailService.sendEmail(TemplateName.USER_UPDATED, new SendEmailService.Recipient(user));

        return update;
    }
}
