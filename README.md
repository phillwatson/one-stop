
# One-Stop
A PoC for interacting with the Nordigen Open-Banking API. Attempts to
allow users connect to their bank accountDetails and view the transactions in
an aggregation.

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
The following must be added to ```environment:``` section of the docker compose
services.
### User and Rail Services
```
// the secret used to generate and verify the XSRF token
ONE_STOP_AUTH_XSRF_SECRET: <any string value 18+ chars>
```
### Rail Service
```
// the Nordigen authentication keys
RAILS_SECRET_ID: <the secret ID issued by Nordigen>
RAILS_SECRET_KEY: <the secret issue by Nordigen>

// the callback URL after user consent (or denial)
ONE_STOP_RAIL_CALLBACK_URL: https://hillayes.com/api/v1/consents/response
```

## To build and start docker image
```
mvn clean package
docker compose up -d
```
