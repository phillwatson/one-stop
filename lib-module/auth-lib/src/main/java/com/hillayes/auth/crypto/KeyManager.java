package com.hillayes.auth.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.Cipher;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class KeyManager {
    private static final String KEY_ALGORITHM = "ECDH";
    private static final String CIPHER_ALGORITHM = "ECIES";
    private static final AlgorithmParameterSpec KEYGEN_PARAMETER_SPEC = new ECGenParameterSpec("P-256");
    private static final X500Name CERT_X500_NAME = new X500Name("C=UK,ST=Wiltshire,O=Hillayes,OU=com");

    private final KeyStoreFactory keyStoreFactory;

    private static final Provider bouncyCastleProvider = new BouncyCastleProvider();
    static {
        Security.addProvider(bouncyCastleProvider);
    }

    public PrivateKey getPrivateKey(String alias, char[] password) throws IOException, GeneralSecurityException {
        KeyStore keyStore = keyStoreFactory.loadKeyStore();
        return (PrivateKey)keyStore.getKey(alias, password);
    }

    public PublicKey getPublicKey(String alias) throws IOException, GeneralSecurityException {
        KeyStore keyStore = keyStoreFactory.loadKeyStore();
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public KeyPair newKeyPair(String alias, char[] password) throws IOException, GeneralSecurityException, OperatorCreationException {
        KeyStore keyStore = keyStoreFactory.loadKeyStore();
        try {
            KeyPair keyPair = generateKeyPair();
            Certificate cert = getCertificate(keyPair);
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[] { cert });

            return keyPair;
        } finally {
            keyStoreFactory.saveKeyStore(keyStore);
        }
    }

    public String encrypt(String alias, String inputData) throws GeneralSecurityException, IOException {
        return encrypt(getPublicKey(alias), inputData);
    }

    public String encrypt(PublicKey publicKey, String inputData) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encoded = cipher.doFinal(inputData.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(encoded));
    }

    public  String decrypt(String alias, char[] password, String encrypted) throws GeneralSecurityException, IOException {
        return decrypt(getPrivateKey(alias, password), encrypted);
    }

    public  String decrypt(PrivateKey privateKey, String encrypted) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] inputData = Base64.getDecoder().decode(encrypted.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(inputData));
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(KEYGEN_PARAMETER_SPEC, new SecureRandom());

        return generator.generateKeyPair();
    }

    private Certificate getCertificate(KeyPair keyPair) throws GeneralSecurityException, OperatorCreationException, CertIOException {
        Instant now = Instant.now();
        long notBefore = now.toEpochMilli();
        long notAfter = now.plus(Duration.ofDays(365 * 30)).toEpochMilli();

        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
            CERT_X500_NAME, BigInteger.valueOf(notBefore),
            new Date(notBefore), new Date(notAfter),
            CERT_X500_NAME, keyPair.getPublic()
//        ).addExtension(
//            // This extension indicates if the subject may act as a CA, with the
//            // certified public key being used to verify certificate signatures.
//            // If so, a certification path length constraint may also be specified.
//            new ASN1ObjectIdentifier("2.5.29.19"),
//            false,
//            new BasicConstraints(false) // true if it is allowed to sign other certs
//        ).addExtension(
//            // This extension indicates the purpose for which the certified public key is used.
//            new ASN1ObjectIdentifier("2.5.29.15"),
//            true,
//            new X509KeyUsage(
//                X509KeyUsage.digitalSignature | X509KeyUsage.nonRepudiation |
//                    X509KeyUsage.keyEncipherment  | X509KeyUsage.dataEncipherment)
        );

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
            .setProvider(bouncyCastleProvider)
            .getCertificate(certificateBuilder.build(contentSigner));
    }
}
