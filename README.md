
# One-Stop

---
A PoC for interacting with the Nordigen Open-Banking API. Attempts to
allow users connect to their bank accountDetails and view the transactions
from multiple accounts in an aggregation.

## Architecture
One-Stop has been designed using a micro-service architecture, where
each service has a specific area of responsibility. However, to keep the
PoC build simple, the Maven model of parent POM and sub-modules has been
adopted; with each module adopting the same version as the parent.

In a production environment it would be more appropriate to separate each
module (library or service) into its own project, and have each project
follow its own versioning history. Dependant modules would then adopt the
version of the project on which they depend - updating their dependencies
as and when necessary.

## Docker Configuration
The following must be added to ```environment:``` section of the docker-compose.yaml
services. This can be also be achieved by creating a docker-compose-override.yaml.
### User and Rail Services
```
// the secret used to generate and verify the XSRF token
ONE_STOP_AUTH_XSRF_SECRET: <any string value 18+ chars>

// the secret used to authenticate with Google Open-ID Connect
ONE_STOP_AUTH_OPENID_GOOGLE_CLIENT_SECRET: <the secret issued by Google>
```
### Rail Service
```
// the Nordigen authentication keys
RAILS_SECRET_ID: <the secret ID issued by Nordigen>
RAILS_SECRET_KEY: <the secret issue by Nordigen>

// the callback URL after user consent (or denial)
ONE_STOP_RAIL_CALLBACK_URL: https://hillayes.com/api/v1/consents/response
```
### Email Service
```
// the Brevo (SendInBlue) Email-Service key
ONE_STOP_EMAIL_API_KEY: <the secret issue by Brevo>

// to disable the sending of emails - default false
ONE_STOP_EMAIL_DISABLED: true
```

## To Build and Start Docker Images
```
mvn clean package
docker compose up -d
```
### Building Client Docker Image
The client docker image is not built by the maven POM. To build the client create
a docker-compose-override.yaml and include the following - any existing docker
image should be deleted first:
```
services:
  client:
    image: one-stop/client:1.0.0-SNAPSHOT
    build:
      context: ./client
```

### Build Parameters
By default, the build does not run unit-tests. To run the unit-tests, add
the following parameter to the build command:
```
mvn clean package -Punit
```
By default, the client is not built. To build the client, add the following
parameter to the build command:
```
mvn clean package -Pclient
```

## Debugging Docker Images
All docker images are built with remote JVM debugging enabled. In order to
connect to the images the debug port 5005 must be exposed. Each container 
should expose a unique port - to avoid clashes.
