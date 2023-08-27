
# One-Stop

---
## User-Service
This micro-service provides the user account administration. It co-ordinates the
registration, on-boarding, authentication and deletion of user accounts. All actions
related to the user account are published as events so that they can be audited.

As only the user account identity is passed in the authentication JWT, those
services that need more information about users invoking their endpoints (such as
their name or email address) must listen to those events and maintain their own
user records - remembering to delete them when a user-deleted event is issued.

## Authentication
### Access and Refresh Tokens
Upon successful authentication, each response to the client will contain an access
and refresh token; passed cookies. The client is not able to read these cookies,
but they will be returned on each request.

The refresh token will only be returned to the token refresh endpoint.

#### Token Security
In addition to marking the cookies as `Same-Site=Strict`, `Secure=true` and
`Http-Only=true`, the tokens with the cookies are signed using "rotating" Private
Keys; and carry the Private Key ID in the header claim "kid".

The number of Private Keys and the frequency at which they are renewed are
determined by the configuration properties; `"one-stop.auth.jwk.set-size"` and
`"one-stop.auth.jwk.rotation-interval"`, respectively.

These properties also determine the period for which any signed data is valid. For
example; a number of 3 and interval of 30 seconds would mean any data signed by a
private key is valid for, at least, 60 seconds and, at most 90 seconds.

It is important that the refresh tokens have a lifespan that divides equally into
the minimum validity period of the Private Keys.

In order to perform their own verification of the signed tokens, clients (such as
other micro-services) can obtain the list of Public Keys as JSON Web Keys (JWKS)
using the User Service endpoint `"/api/v1/auth/jwks.json"`.

#### Token Content
The tokens carry various claims on which they are later verified. The main claims
are issuer ("iss") and audience ("aud"). These values are determined by the config
properties; `"one-stop.auth.access-token.issuer"` and `"one-stop.auth.access-token.audiences"`.
Note that the audience can be comma-delimited list of values.

The other verification claim is the Expiry Time ("exp"). This is defined by the
configuration properties; `"one-stop.auth.access-token.expires-in"` and
`"one-stop.auth.refresh-token.expires-in"`.

The tokens also carry the identity of the user (the UUID of the User record) in the
User Principal Name ("upn") claim.

In addition, the access token carries the named of the user's authorisation roles
as comma-delimited list within the Groups ("groups") claim. These are used to
authorise the user's access to a service's endpoints.

#### Token Expiry
The tokens (and cookies) have expiry times defined by the configuration properties;
`"one-stop.auth.access-token.expires-in"` and `"one-stop.auth.refresh-token.expires-in"`.

The access token expiry determine the frequency at which the user must refresh the
tokens, and the refresh token expiry determines the maximum duration of client
inactivity before the session is invalidated.

### Cross-Site Resource Forgery (XSRF)
Each authenticated response from the server will carry an XSRF cookie containing a
cryptographically signed random value. The same value is also contained in the claims
of the access and refresh cookies. The client must return this value in the headers
of its requests. 

For authenticated endpoints, the server will ensure the presence of the XSRF header,
and verify that its signature is correct. It then compares the value with that in
the access token claims. When refreshing the tokens, the value is compared with that
in the refresh token claims.

The name of the XSRF cookie and header are, by default, `"XSRF-TOKEN"` and `"X-XSRF-TOKEN"`,
respectively. These names can be overridden using the config properties;
`"one-stop.auth.xsrf.cookie"` and `"one-stop.auth.xsrf.header"`.

As the XSRF cookie is verified when the auth tokens are refreshed, the cookie has the
same expiry time as the refresh token.
