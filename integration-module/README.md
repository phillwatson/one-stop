

Current issue given "Out of Heap Memory" exception when building client container.
This is due to testcontainers attempting to copy the entire client folder
(including node_modules) into the container.

https://github.com/testcontainers/testcontainers-java/issues/7239

Preferable solution would be to use .dockerignore files to exclude ALL client
module files; and create a docker image for the api-gateway (based on nginx).
