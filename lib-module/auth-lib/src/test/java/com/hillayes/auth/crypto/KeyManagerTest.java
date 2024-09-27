package com.hillayes.auth.crypto;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeyManagerTest {
    KeyStoreFactory keyStoreFactory;

    KeyManager keyManager;

    @BeforeEach
    public void beforeEach() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        keyStoreFactory = mock();
        when(keyStoreFactory.loadKeyStore()).thenReturn(keyStore);

        keyManager = new KeyManager(keyStoreFactory);
    }

    @Test
    public void testNewKeyPair() throws Exception {
        KeyPair keyPair = keyManager.newKeyPair("key-1", "password".toCharArray());
        assertNotNull(keyPair);

        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
    }

    @Test
    public void testEncryption1() throws Exception {
        keyManager.newKeyPair("key-1", "key-pass".toCharArray());

        String inputData = RandomStringUtils.randomAlphanumeric(128);
        String encrypted = keyManager.encrypt(keyManager.getPublicKey("key-1"), inputData);
        String decrypted = keyManager.decrypt(keyManager.getPrivateKey("key-1", "key-pass".toCharArray()), encrypted);

        assertEquals(inputData, decrypted);
    }

    @Test
    public void testEncryption2() throws Exception {
        keyManager.newKeyPair("key-1", "key-pass".toCharArray());

        String inputData = RandomStringUtils.randomAlphanumeric(128);
        String encrypted = keyManager.encrypt("key-1", inputData);
        String decrypted = keyManager.decrypt("key-1", "key-pass".toCharArray(), encrypted);

        assertEquals(inputData, decrypted);
    }

    @Test
    public void testEncryption3() throws Exception {
        KeyPair keyPair = keyManager.newKeyPair("key-1", "key-pass".toCharArray());

        String inputData = RandomStringUtils.randomAlphanumeric(128);
        String encrypted = keyManager.encrypt(keyPair.getPublic(), inputData);
        String decrypted = keyManager.decrypt(keyPair.getPrivate(), encrypted);

        assertEquals(inputData, decrypted);
    }
}
