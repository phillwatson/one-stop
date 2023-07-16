
# One-Stop

---
## User-Service
This micro-service provides the user account administration. It co-ordinates the
registration, on-boarding, authentication and deletion of user accounts. All actions
related to the user account are published as events so that they can be audited.

As only the user account identity is passed in the authentication JWT, those
services that need more information about users invoking their endpoints (such as
their name or email address) must listen to those events and maintain their own
user records - remembering to delete them when a delete event is received.
