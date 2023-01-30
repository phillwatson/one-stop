

To start docker image
```
docker run -i --rm -p 8080:8080 one-stop/rail-service:1.0-SNAPSHOT
```

The generate auth key pairs:
```
openssl genrsa -out rsaPrivateKey.pem 2048
openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
```
