
# One-Stop

---
## Notification-Service
This microservice is responsible for issuing notification (incl. emails)  in
response to various published events such as; user-created, user-deleted, consent-given.

In order to issue notifications to users, it maintains its own record of users by
listening for user life-cycle events.

### Templates

#### Configuration
The notifications templates are configured within the `application.yaml`, and
map to the enum `com.hillayes.notification.domain.NotificationId`.
The following is an example of the template configuration for the notifications
`NotificationId.CONSENT_SUSPENDED` and `NotificationIdCONSENT_EXPIRED`.
```yaml
one-stop:
  notification:
    templates:
      consent-suspended:
        en: "Access to $event.institutionName$ has been suspended.\nYou need to renew your consent."
      consent-expired:
        en: "Access to $event.institutionName$ has expired.\nYou need to renew your consent."
```

The email subject and templates are also configured within the `application.yaml`,
and map to the enum `com.hillayes.notification.config.TemplateName`.
The following is an example of the template configuration for
`TemplateName.USER_REGISTERED`.
```yaml
one-stop:
  email:
    templates:
      user-registered:
        en:
          subject: "Hi $user.preferredName$, please complete your One-Stop registration"
          template: "user-registered/en.html"
        fr:
          subject: "Salut $user.preferredName$, veuillez compléter votre inscription One-Stop"
          template: "user-registered/fr.html"
```

The example shows two locale variations for the template; English and French. The
chosen locale will be based on the locale of the recipient User record.

Each variation identifies a subject and body. The body references a file that can
be located using `java.lang.class.getResourceAsStream(String)`; normally with the
`resources` folder.

#### Header and Footer
The template body file will contain only the body particular to the email subject.
Any header and footer content will be taken from the "special" `TemplateName.HEADER`,
which may also be chosen by locale.

#### Template Args
Based on the Antlr ST4 template library, the notification and email templates
may reference any property in a given Map of key/values. Templates may also
reference properties of the value in the Map. For example; if the Map contains
a reference the interface (or class), with the key `"account"`:
```Java
  public interface Account {
    public String getName();
    public String getBic();
  } 
```
Then the template may reference the BIC code for this account using `$account.bic$`.

#### Common Args
Some properties are common to all templates, and are therefore defined in the
`application.yaml` under the `one-stop.email.common-args` key. These properties
can be used to provide the company name, address, and contact details.
```yaml
one-stop:
  email:
    common-args:
      COMPANY_LONG_NAME: Hillayes One-Stop
      COMPANY_COPYRIGHT_NAME: One-Stop
      COMPANY_CONTACT_EMAIL: admin@one-stop.com
      COMPANY_ADDRESS_LINE1: Hillayes
      COMPANY_ADDRESS_LINE2: Hullavington
      COMPANY_ADDRESS_LINE3: Wiltshire
```

#### Recipient
Email are normally issued in response to an event, and the email recipient is
the User identified by the event payload, or by the email address provided in
the event payload.

For those occasions when the email is to be sent to a non-User (e.g. an administrator),
the recipient can be defined in the template.
```yaml
one-stop:
  email:
    templates:
      message-hospital:
        en:
          subject: "An event has failed to be processed"
          template: "event-failure/en.html"
          recipient:
            name: "Administrator"
            email: "admin@one-stop.com"
            locale: "en"  
```

#### Sender
Each template may also define a sender, to be identified in the email header.
```yaml
one-stop:
  email:
    templates:
      user-registered:
        en:
          subject: "Hi $user.preferredName$, please complete your One-Stop registration"
          template: "user-registered/en.html"
          sender:
            name: "Administrator"
            email: "admin@one-stop.com"
            locale: "en"  
```

If no sender is provided in the template, the `default-sender` is used.
```yaml
one-stop:
  email:
    default-sender:
      name: ${one-stop.email.common-args.COMPANY_LONG_NAME}
      email: ${one-stop.email.common-args.COMPANY_CONTACT_EMAIL}
      locale: "en"
```

#### Disabling Emails
The email service can be disabled by setting the `one-stop.email.disabled` property
to `true`. This will prevent any emails from being sent, but notifications will still
be issued.
```yaml
one-stop:
  email:
    disabled: false
```
