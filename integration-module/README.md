# One-Stop

---
## Integration Module

Starts the application (and services) in docker containers and runs tests against them.
The 3rd party services (e.g. email and rails) are simualuted using Wiremock.

### issues
Current issue given "Out of Heap Memory" exception when building client container.
This is due to testcontainers attempting to copy the entire client folder
(including node_modules) into the container.

see: https://github.com/testcontainers/testcontainers-java/issues/7239

Preferable solution would be to use .dockerignore files to exclude ALL client
module files; and create a docker image for the api-gateway (based on nginx).
