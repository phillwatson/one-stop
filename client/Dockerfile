FROM nginx:1.17.0-alpine

# Copy the react build output
COPY build /etc/nginx/html/

# Copy our custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Expose port 80 to the Docker host, so we can access it from the outside.
EXPOSE 80

ENTRYPOINT ["nginx","-g","daemon off;"]
