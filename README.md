
# One-Stop

---
A PoC for interacting with the Open-Banking APIs. Allows users connect to
their bank accounts and view the transactions from multiple accounts in an
aggregated fashion.

## Architecture
One-Stop has been designed using a modulith service architecture, where
each service has a specific area of responsibility. The Maven model of parent
POM and sub-modules has been adopted; with each module adopting the same
version as the parent.

### Structure
The project consists of a parent POM with a number of sub-modules. The sub-modules
consist of services (that provide client functionality) and libraries (that provide
internal functionality shared by services and other libraries).

In general, and to reduce clutter, the library modules are grouped under the parent
module `lib-module`. The exception to this is the event libraries; which are grouped
under the parent module `event-module`.


## Docker Configuration
The following must be added to `environment:` section of the docker-compose.yaml
services. This can be also be achieved by creating a docker-compose-override.yaml.

### User Services
```yaml
# the http schema to use (http or https)
ONE_STOP_GATEWAY_SCHEME: http

# the port exposed on the router to the outside world
# used by callback from 3rd party services (e.g. rails)
ONE_STOP_GATEWAY_OPEN_PORT: 9876

# the secret used to generate and verify the XSRF token
ONE_STOP_AUTH_XSRF_SECRET: <any string value 18+ chars>

# the secret used to authenticate with Google Open-ID Connect
ONE_STOP_AUTH_OPENID_GOOGLE_CLIENT_SECRET: <the secret issued by Google>

# the secret used to authenticate with GitHub Open-ID Connect
ONE_STOP_AUTH_OPENID_GITHUB_CLIENT_SECRET: "<the secret issued by GitHub>"

# the secret used to authenticate with GitLab Open-ID Connect
ONE_STOP_AUTH_OPENID_GITLAB_CLIENT_SECRET: "<the secret issued by GitLab>"

# the App Key-ID used to authenticate with Apple Open-ID Connect
ONE_STOP_AUTH_OPENID_APPLE_KEY_ID: "<App key id issued by Apple>"

# the Private Key PEM used to sign data sent to Apple Open-ID Connect
ONE_STOP_AUTH_OPENID_APPLE_PRIVATE_KEY: "<the private key in PEM form>"
```

### Rail Service
The application relies upon the bank data provided by the Nordigen service
(now owned by GoCardless). Sign-up and access is free:
https://gocardless.com/bank-account-data/ 

The application also supports Yapily. Sandbox sign-up and access is free:
https://docs.yapily.com/
```yaml
# the secret used to generate and verify the XSRF token
ONE_STOP_AUTH_XSRF_SECRET: <any string value 18+ chars - must be same as user service>

# the Nordigen/GoCardless authentication keys
ONE_STOP_RAILS_SECRET_ID: <the secret ID issued by Nordigen>
ONE_STOP_RAILS_SECRET_KEY: <the secret issue by Nordigen>

# the Yapily authentication keys
ONE_STOP_YAPILY_SECRET_ID: <the secret ID issued by Yapily>
ONE_STOP_YAPILY_SECRET_KEY: <the secret issue by Yapily>
```

### Notification Service
In order to use the notification service, you will need to obtain an API key from
Brevo (previously known as Send-With-Blue). Sign-up and access is free:
https://www.brevo.com/
```yaml
# the Brevo (SendInBlue) Email-Service key
ONE_STOP_EMAIL_API_KEY: <the secret issue by Brevo (previosly Send-With-Blue)>

# to disable the sending of emails - default false
ONE_STOP_EMAIL_DISABLED: true
```

## To Build and Start Docker Images
```shell
mvn clean package
docker compose up -d
```
### Building Client Docker Image
The client docker image is not built by the maven POM. To build the client create
a docker-compose-override.yaml and include the following - any existing docker
image should be deleted first:
```yaml
services:
  client:
    image: one-stop/client:1.0.0-SNAPSHOT
    build:
      context: ./client
```

### Build Parameters
By default, the build does not run unit-tests. To run the unit-tests, add
the following parameter to the build command:
```shell
mvn clean package -Ptest
```
By default, the client is not built. To build the client, add the following
parameter to the build command:
```shell
mvn clean package -Pclient
```

By default, the docker images use JVM base images. To build native docker
images, add the following parameter to the build command:
```shell
mvn clean package -Dnative
```
Combinations of these can be used.

## Debugging Docker Images
All non-native docker images are built with remote JVM debugging enabled. In
order to connect to the images the debug port 5005 must be exposed. Each
container should expose a unique port - to avoid clashes.
```yaml
  one-stop-service:
    image: one-stop/one-stop-main:1.0.0-SNAPSHOT
    ports:
      - "8081:8080"
      - "5001:5005"
```
