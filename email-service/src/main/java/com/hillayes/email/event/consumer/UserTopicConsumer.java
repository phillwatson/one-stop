package com.hillayes.email.event.consumer;

import com.hillayes.email.config.TemplateName;
import com.hillayes.email.domain.User;
import com.hillayes.email.service.SendEmailService;
import com.hillayes.email.service.SendEmailService.Recipient;
import com.hillayes.email.service.UserService;
import com.hillayes.events.annotation.TopicConsumer;
import com.hillayes.events.consumer.EventConsumer;
import com.hillayes.events.domain.EventPacket;
import com.hillayes.events.domain.Topic;
import com.hillayes.events.events.user.UserCreated;
import com.hillayes.events.events.user.UserDeleted;
import com.hillayes.events.events.user.UserRegistered;
import com.hillayes.events.events.user.UserUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@ApplicationScoped
@TopicConsumer(Topic.USER)
@RequiredArgsConstructor
@Slf4j
public class UserTopicConsumer implements EventConsumer {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss, dd MMM yyyy vvvv");

    private final UserService userService;
    private final SendEmailService sendEmailService;

    @Transactional
    public void consume(EventPacket eventPacket) throws Exception {
        String payloadClass = eventPacket.getPayloadClass();
        log.info("Received user event [payloadClass: {}]", payloadClass);

        if (UserRegistered.class.getName().equals(payloadClass)) {
            processUserRegistered(eventPacket.getPayloadContent());
        } else if (UserCreated.class.getName().equals(payloadClass)) {
            processUserCreated(eventPacket.getPayloadContent());
        } else if (UserUpdated.class.getName().equals(payloadClass)) {
            processUserUpdated(eventPacket.getPayloadContent());
        } else if (UserDeleted.class.getName().equals(payloadClass)) {
            processUserDeleted(eventPacket.getPayloadContent());
        }
    }

    private void processUserRegistered(UserRegistered event) throws Exception {
        Recipient recipient = new Recipient(event.getEmail(), event.getEmail());
        Map<String, Object> params = Map.of(
            "acknowledge-uri", event.getAcknowledgerUri(),
            "expires", format(event.getExpires())
        );
        sendEmailService.sendEmail(TemplateName.USER_REGISTERED, recipient, params);
    }

    private void processUserCreated(UserCreated event) throws Exception {
        User user = User.builder()
            .id(event.getUserId())
            .username(event.getUsername())
            .email(event.getEmail())
            .title(event.getTitle())
            .givenName(event.getGivenName())
            .familyName(event.getFamilyName())
            .preferredName(event.getPreferredName())
            .build();
        user = userService.createUser(user);

        sendEmailService.sendEmail(TemplateName.USER_CREATED, new Recipient(user));
    }

    private void processUserDeleted(UserDeleted event) {
        userService.deleteUser(event.getUserId());
    }

    private void processUserUpdated(UserUpdated event) {
        User user = User.builder()
            .id(event.getUserId())
            .username(event.getUsername())
            .email(event.getEmail())
            .title(event.getTitle())
            .givenName(event.getGivenName())
            .familyName(event.getFamilyName())
            .preferredName(event.getPreferredName())
            .build();
        userService.updateUser(user);
    }

    private String format(Instant dateTime) {
        return DATE_TIME_FORMATTER.format(ZonedDateTime.ofInstant(dateTime, ZoneId.systemDefault()));
    }
}
