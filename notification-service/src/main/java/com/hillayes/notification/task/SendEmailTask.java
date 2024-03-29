package com.hillayes.notification.task;

import com.hillayes.executors.scheduler.TaskContext;
import com.hillayes.executors.scheduler.tasks.AbstractNamedJobbingTask;
import com.hillayes.executors.scheduler.tasks.TaskConclusion;
import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.domain.User;
import com.hillayes.notification.service.SendEmailService;
import com.hillayes.notification.service.UserService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Queues a task to send email to the identified recipient.
 * This allows events processing to offload the sending of emails to a background
 * thread whilst still supporting retries should the send fail.
 */
@ApplicationScoped
@Slf4j
public class SendEmailTask extends AbstractNamedJobbingTask<SendEmailTask.Payload> {
    private final UserService userService;
    private final SendEmailService sendEmailService;

    public SendEmailTask(UserService userService,
                         SendEmailService sendEmailService) {
        super("queue-email");
        this.userService = userService;
        this.sendEmailService = sendEmailService;
    }

    /**
     * Sends an email using the specified template and parameters. As no recipient
     * is provided, the template configuration is assumed to provide a default recipient.
     * @param templateName the email template to use
     * @param params the parameters to use in the template.
     * @return the id of the queued job that will send the email.
     */
    public String queueJob(TemplateName templateName,
                           Map<String, Object> params) {
        return queueJob((UUID) null, templateName, params);
    }

    /**
     * Sends an email using the specified template and parameters. The identified
     * user will be the recipient.
     * @param userId the id of the user to send the email to.
     * @param templateName the email template to use
     * @param params the parameters to use in the template.
     * @return the id of the queued job that will send the email.
     */
    public String queueJob(UUID userId,
                           TemplateName templateName,
                           Map<String, Object> params) {
        log.info("Queuing email job [template: {}]", templateName);
        Payload payload = Payload.builder()
            .userId(userId)
            .templateName(templateName)
            .params(params)
            .build();
        return scheduler.addJob(this, payload);
    }

    /**
     * Sends an email using the specified template and parameters. The identified
     * recipient will be used.
     * @param recipient the recipient of the email.
     * @param templateName the email template to use
     * @param params the parameters to use in the template.
     * @return the id of the queued job that will send the email.
     */
    public String queueJob(EmailConfiguration.Corresponder recipient,
                           TemplateName templateName,
                           Map<String, Object> params) {
        log.info("Queuing email job [template: {}]", templateName);
        Payload payload = Payload.builder()
            .recipient(new EmailRecipient(recipient))
            .templateName(templateName)
            .params(params)
            .build();
        return scheduler.addJob(this, payload);
    }

    @Transactional
    @Override
    public TaskConclusion apply(TaskContext<Payload> payloadTaskContext) {
        Payload payload = payloadTaskContext.getPayload();

        Map<String, Object> params = new HashMap<>();

        // use any recipient in the payload, otherwise try to identify the user
        EmailRecipient recipient = payload.recipient;

        // if the payload identifies a user
        if (payload.userId != null) {
            // try to identify the user
            User user = userService.getUser(payload.userId).orElse(null);
            if (user != null) {
                // add user details to the template parameters
                params.put("user", user);

                // if payload doesn't have a recipient, use the user as the recipient
                if (recipient == null) {
                    recipient = new EmailRecipient(user);
                }
            }
        }

        if (payload.params != null) {
            params.putAll(payload.params);
        }

        // if no recipient is identified, the template may provide a default
        EmailConfiguration.Corresponder corresponder = (recipient == null) ? null : recipient.toCorresponder();

        // send the email
        sendEmailService.sendEmail(payload.templateName, corresponder, params);
        return TaskConclusion.COMPLETE;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @RegisterForReflection
    public static class Payload {
        UUID userId;
        EmailRecipient recipient;
        TemplateName templateName;
        Map<String, Object> params;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @RegisterForReflection
    public static class EmailRecipient {
        String email;
        String name;
        Locale locale;

        EmailRecipient(User user) {
            this(user.getEmail(),
                user.getPreferredName() != null ? user.getPreferredName() : user.getGivenName(),
                user.getLocale());
        }

        EmailRecipient(EmailConfiguration.Corresponder corresponder) {
            this(corresponder.getEmail(), corresponder.getName(), corresponder.getLocale().orElse(null));
        }

        public EmailConfiguration.Corresponder toCorresponder() {
            return new SendEmailService.Recipient(email, name, locale);
        }
    }
}
