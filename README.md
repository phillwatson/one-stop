
# One-Stop
A PoC for interacting with the Nordigen Open-Banking API. Attempts to
allow users connect to their bank accounts and view the transactions in
an aggregation.

## Docker Configuration
The following must be added to ```environment:``` section of the docker compose
services.
### All Services
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
