package com.hillayes.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.exception.common.NotFoundException;
import com.hillayes.commons.correlation.Correlation;
import com.hillayes.notification.config.NotificationConfiguration;
import com.hillayes.notification.domain.Notification;
import com.hillayes.notification.domain.NotificationId;
import com.hillayes.notification.errors.NotificationIdNotFoundException;
import com.hillayes.notification.errors.ParameterDeserialisationException;
import com.hillayes.notification.errors.ParameterSerialisationException;
import com.hillayes.notification.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.stringtemplate.v4.ST;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static final TypeReference<HashMap<String, Object>> PARAMS_MAP = new TypeReference<>() {
    };

    private final ObjectMapper jsonMapper;
    private final NotificationConfiguration configuration;
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    /**
     * Creates a Notification to record a message issued to the identified user.
     * The Notification records the message context, in the form of name/value
     * attributes, and also the correlation ID in which the notification was
     * issued.
     *
     * @param userId the user to which the notification relates.
     * @param timestamp the date-time when the notification was originally issued.
     * @param notificationId the identifier that provides notification details.
     * @param params the context attributes to be included in rendered message.
     * @return the new Notification record.
     * @throws ParameterSerialisationException
     */
    public Notification createNotification(UUID userId,
                                           Instant timestamp,
                                           NotificationId notificationId,
                                           Map<String, Object> params) throws ParameterSerialisationException {
        // a notification may already exist
        return notificationRepository.findByUserAndTimestamp(userId, timestamp, notificationId).or(() -> {
            try {
                // create a new record
                Notification notification = Notification.builder()
                    .userId(userId)
                    .correlationId(Correlation.getCorrelationId().orElse(null))
                    .dateCreated(timestamp)
                    .messageId(notificationId)
                    .attributes(params == null ? null : jsonMapper.writeValueAsString(params))
                    .build();

                return Optional.of(notificationRepository.save(notification));
            } catch (JsonProcessingException e) {
                throw new ParameterSerialisationException(notificationId, e);
            }
        }).get();
    }

    /**
     * Returns a list of Notification for the identified user issued after the
     * given timestamp, in the order they were issued. Each entry in the result
     * is constructed by passing the given Notification and its rendered message
     * to the given NotificationMapper.
     *
     * @param userId the identifier for the user to whom the notifications belong.
     * @param after the timestamp that all notification must proceed.
     * @param mapper the NotificationMapper used to construct the resulting entities.
     * @param <T> the type of the mapped notification entities.
     * @return the collection of mapped notifications in the order they were issued.
     */
    public <T> List<T> listNotifications(UUID userId, Instant after,
                                         NotificationMapper<T> mapper) {
        log.info("List notifications [userId: {}, after: {}]", userId, after);

        return userService.getUser(userId)
            .map(user -> {
                Optional<Locale> locale = Optional.ofNullable(user.getLocale());
                List<T> notifications = notificationRepository.listByUserAndTime(userId, after)
                    .stream()
                    .map(notification -> mapper.map(notification, renderMessage(notification, locale)))
                    .toList();

                log.debug("List notifications [userId: {}, after: {}, count: {}]", userId, after, notifications.size());
                return notifications;
            })
            .orElse(List.of());
    }

    public void deleteNotification(UUID userId, UUID notificationId) {
        log.info("Deleting user notification [userId: {}, notificationId: {}]", userId, notificationId);

        Notification notification = notificationRepository.findById(notificationId);
        if (notification == null) {
            log.debug("Notification not found [notificationId: {}]", notificationId);
            return;
        }

        if (!notification.getUserId().equals(userId)) {
            throw new NotFoundException("Notification", notificationId);
        }

        notificationRepository.delete(notification);
    }

    /**
     * Reads the notification message from the identified notification configuration
     * and renders it with the given parameter/attribute values and, finally, returns
     * the result.
     *
     * @param notification the notification to be rendered.
     * @param locale the optional locale used to determine the language in which the
     *     message is to be rendered. If not given, the default system locale is
     *     used.
     * @return the rendered message.
     * @throws NotificationIdNotFoundException if the message template cannot be found.
     */
    private String renderMessage(Notification notification,
                                 Optional<Locale> locale) throws NotificationIdNotFoundException {
        // find the message template from the notification configuration
        String subject = selectByLocale(notification.getMessageId(), locale.orElse(DEFAULT_LOCALE));

        ST engine = new ST(subject, '$', '$');

        // apply any common parameters to the message template
        if (configuration.commonArgs() != null) {
            configuration.commonArgs().forEach(engine::add);
        }

        // apply notification's own parameters to the message template
        if (notification.getAttributes() != null) {
            try {
                HashMap<String, Object> params = jsonMapper.readValue(notification.getAttributes(), PARAMS_MAP);
                params.forEach(engine::add);
            } catch (JsonProcessingException e) {
                throw new ParameterDeserialisationException(notification.getMessageId(), e);
            }
        }

        // return rendered result
        return engine.render();
    }

    /**
     * Attempts to locate the notification from the given name that best fits
     * the given locale. If no exact match can be found, a look-up is performed
     * using the given locale's language alone. If still no match, the default
     * locale is used.
     *
     * @param notificationId the notification message identifier.
     * @param locale the locale for which a message is required.
     * @return the message that best fits the given locale.
     * @throws NotificationIdNotFoundException if no message can be found.
     */
    private String selectByLocale(NotificationId notificationId, Locale locale) {
        NotificationConfiguration.MessageConfig messageConfig = configuration.templates().get(notificationId);
        if (messageConfig == null) {
            throw new NotificationIdNotFoundException(notificationId);
        }

        String result = messageConfig.templates().get(locale);
        if (result == null) {
            result = messageConfig.templates().get(new Locale(locale.getLanguage()));
        }

        if (result == null) {
            result = messageConfig.templates().get(DEFAULT_LOCALE);
        }

        if (result == null) {
            result = messageConfig.templates().get(new Locale(DEFAULT_LOCALE.getLanguage()));
        }

        if (result == null) {
            throw new NotificationIdNotFoundException(notificationId);
        }
        return result;
    }

    public interface NotificationMapper<T> {
        public T map(Notification notification, String message);
    }
}
