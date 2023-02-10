
## Configuration
```
// the Nordigen authentication keys
RAILS_SECRET_ID: <the secret ID issued by Nordigen>
RAILS_SECRET_KEY: <the secret issue by Nordigen>

// the callback URL after user consent (or denial)
ONE_STOP_CALLBACK_URL: http://5.81.68.243/api/v1/consents/response
```

### To build and start docker image
```
mvn clean package
docker compose up -d
```

### The generate auth key pairs:
```
openssl genrsa -out rsaPrivateKey.pem 2048
openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
```
