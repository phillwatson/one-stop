
# One-Stop

---
## Email-Service
This microservice is responsible for sending emails in response to various
published events such as; user-created, user-deleted, consent-given.

In order to send emails to users, it maintains its own record of users by
listening for user life-cycle events.

### Templates

#### Configuration
The email subject and templates are configured within the `application.yaml`,
and map to the enum `com.hillayes.email.config.TemplateName`.
The following is an example of the template configuration for
`TemplateName.USER_REGISTERED`.
```
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
Based on the Antlr ST4 template library, the email subject and body templates
may reference any property in a given Map of key/values. Templates may also
reference properties of the value in the Map. For example; if the Map contains
a reference the interface (or class), with the key `"account"`:
```
  public interface Account {
    public String getName();
    public String getBic();
  } 
```
Then the template may reference the BIC code for this account using `$account.bic$`.
