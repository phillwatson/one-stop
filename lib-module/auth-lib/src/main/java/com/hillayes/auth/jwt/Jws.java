package com.hillayes.auth.jwt;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.keys.X509Util;
import org.jose4j.lang.JoseException;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

public class Jws {
    private final static X509Util X509_UTIL = new X509Util();

    public Optional<JwtClaims> validate(String signedPayload,
                                        X509Certificate rootCertificate) throws JoseException, InvalidJwtException, GeneralSecurityException {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(signedPayload);

        List<String> x5c = (List<String>) jws.getHeaders().getObjectHeaderValue("x5c");
        List<X509Certificate> x509Chain = x5c.stream().map(it -> {
            try {
                return X509_UTIL.fromBase64Der(it);
            } catch (JoseException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        // validate key chain
        validateKeyChain(x509Chain, rootCertificate);

        // validate signature
        jws.setKey(x509Chain.get(0).getPublicKey());
        boolean valid = jws.verifySignature();

        // return the body if valid
        return (valid) ? Optional.of(JwtClaims.parse(jws.getPayload())) : Optional.empty();
    }

    private void validateKeyChain(List<X509Certificate> x509Chain,
                                  X509Certificate rootCertificate) throws GeneralSecurityException {
        // check the validity of the certificate chain
        // should be at least 3 certificates
        if (x509Chain.size() < 3) {
            throw new InvalidKeyException("Invalid certificate chain");
        }

        // verify the signature of each certificate in the chain against
        // the public key of the next certificate in the chain
        for (int i = 0; i < x509Chain.size() - 1; i++) {
            x509Chain.get(i).checkValidity();
            x509Chain.get(i).verify(x509Chain.get(i + 1).getPublicKey());
        }

        PublicKey publicKey = rootCertificate.getPublicKey();

        // compare penultimate cert with root cert
        x509Chain.get(x509Chain.size() - 2).verify(publicKey);

        // compare chain root with root cert
        x509Chain.getLast().verify(publicKey);
    }


    private X509Certificate loadRootCertificate(InputStream inputStream) throws CertificateException {
        // download from : https://www.apple.com/certificateauthority/
        //InputStream inputStream = getClass().getResourceAsStream("AppleRootCA-G3.cer");
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(inputStream);
    }
}
