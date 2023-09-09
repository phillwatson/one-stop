package com.hillayes.notification.repository;

import com.hillayes.notification.config.EmailConfiguration;
import com.hillayes.notification.config.TemplateName;
import com.hillayes.notification.errors.EmailTemplateNotFoundException;
import com.hillayes.notification.errors.EmailTemplateReadException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.stringtemplate.v4.ST;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class TemplateRepository {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static final String BASE_PATH = "/templates/";
    private final EmailConfiguration configuration;

    /**
     * Retries the email subject line from the identified template configuration
     * and renders it with the given parameter/attribute values and, finally, returns
     * the result.
     *
     * @param templateName the template identifier.
     * @param params the name/value parameters to be applied to the placeholders in
     *     the subject content.
     * @param locale the optional locale used to determine the language in which the
     *     subject line is to be rendered. If not given, the default system locale
     *     is used.
     * @return the rendered subject line.
     * @throws EmailTemplateNotFoundException if the template cannot be found.
     */
    public String renderSubject(TemplateName templateName,
                                Map<String, ?> params,
                                Optional<Locale> locale) throws EmailTemplateNotFoundException {
        // find the subject line from the email template configuration
        String subject = selectByLocale(templateName, locale.orElse(DEFAULT_LOCALE)).subject();

        // apply parameters to the subject line
        ST template = new ST(subject, '$', '$');
        if (params != null) {
            params.forEach(template::add);
        }

        // return rendered result
        return template.render();
    }

    /**
     * Reads the content of the email template, identified by the given TemplateName,
     * and render it with the given parameter/attribute values and, finally, returns
     * the result.
     * <p>
     * The given locale is used to locate a template of the identified language. If
     * no template of that locale can be found, the default locale will be used.
     *
     * @param templateName the template identifier.
     * @param params the name/value parameters to be applied to the placeholders in
     *     the template content.
     * @param locale the optional locale used to determine the language in which the
     *     email is to be rendered. If not given, the default system locale is used.
     * @return the rendered template content.
     * @throws EmailTemplateNotFoundException if the template cannot be found.
     * @throws EmailTemplateReadException if an error occurs whilst reading or
     *     rendering the template content.
     */
    public String renderTemplate(TemplateName templateName,
                                 Map<String, ?> params,
                                 Optional<Locale> locale) throws EmailTemplateNotFoundException, EmailTemplateReadException {
        try {
            // read the template content
            String templatePath = selectByLocale(templateName, locale.orElse(DEFAULT_LOCALE)).template();
            String content = readResource(BASE_PATH + templatePath);

            // apply parameters to the template
            ST template = new ST(content, '$', '$');
            if (params != null) {
                params.forEach(template::add);
            }

            // return rendered result
            return template.render();
        } catch (FileNotFoundException e) {
            throw new EmailTemplateNotFoundException(templateName, e);
        } catch (IOException e) {
            throw new EmailTemplateReadException(templateName, e);
        }
    }

    private String readResource(String filename) throws IOException {
        InputStream resource = this.getClass().getResourceAsStream(filename);
        if (resource == null) {
            throw new FileNotFoundException(filename);
        }

        try (InputStream content = new BufferedInputStream(resource)) {
            try (Scanner scanner = new Scanner(content, StandardCharsets.UTF_8)) {
                StringBuilder result = new StringBuilder();
                while (scanner.hasNextLine()) {
                    result.append(scanner.nextLine()).append('\n');
                }
                return result.toString();
            }
        }
    }

    /**
     * Attempts to locate the template from the given name that best fits the
     * given locale. If no exact match can be found, a look-up is performed
     * using the given locale's language alone. If still no match, the default
     * locale is used.
     *
     * @param templateName the template identifier.
     * @param locale the locale for which a template is required.
     * @return the template that best fits the given locale, or null if non exists.
     */
    private EmailConfiguration.LocaleTemplate selectByLocale(TemplateName templateName, Locale locale) {
        EmailConfiguration.TemplateConfig templateConfig = configuration.templates().get(templateName);
        if (templateConfig == null) {
            throw new EmailTemplateNotFoundException(templateName);
        }

        EmailConfiguration.LocaleTemplate result = templateConfig.templates().get(locale);
        if (result == null) {
            result = templateConfig.templates().get(new Locale(locale.getLanguage()));
        }
        if (result == null) {
            result = templateConfig.templates().get(DEFAULT_LOCALE);
        }

        if (result == null) {
            throw new EmailTemplateNotFoundException(templateName);
        }
        return result;
    }
}
