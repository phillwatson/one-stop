
# One-Stop

---
## Integration Module
The integration module serves two purposes. It provides a Java API (based on RestAssured)
to the One-Stop services. It also provides integration tests that test the services
in their "production" environment - although 3rd party services (such as rails and email
providers) are represented by WireMock simulators (sim-lib).

### Integration Tests
The tests will run as part of the Maven `verify` phase. All tests extend the class `ApiTestBase`.
That class uses the Test Containers library to start docker containers for all service, client
and simulator images. The tests use the integration API to invoke the services and check their
responses. All requests go via the client API gateway container and use the same auth methods
as a real client would (i.e. JWT and XSRF cookies).

The tests can be run individually (e.g. from within an IDE) but, in that case, they will be run
against whatever docker images are currently available. So, any code changes to the services
will not be tested unless new images are built before running the tests.

### Issues
Current issue giving "Out of Heap Memory" exception when building client container.
This is due to testcontainers attempting to copy the entire client folder (including node_modules)
into the container.

https://github.com/testcontainers/testcontainers-java/issues/7239

Preferable solution would be to use .dockerignore files to exclude ALL client module files; and
create a docker image for the api-gateway (based on nginx).

The current workaround is to delete the `client/node_modules` folder before running integration tests.
This requires that the client `pom.xml` calls the `clean` plugin during the `pre-integration-test`
phase. 
