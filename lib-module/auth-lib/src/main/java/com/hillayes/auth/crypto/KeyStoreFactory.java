package com.hillayes.auth.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@ApplicationScoped
@Slf4j
public class KeyStoreFactory {
    @ConfigProperty(name = "one-stop.crypto.keystore.location", defaultValue = "/var/lib/one-stop/crypto.keystore.jks")
    String keystoreLocation;

    @ConfigProperty(name = "one-stop.crypto.keystore.password")
    String password;

    private volatile KeyStore keyStore = null;

    public KeyStore loadKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore result = keyStore;
        if (result == null) {
            synchronized (KeyStoreFactory.class) {
                result = keyStore;
                if (result == null) {
                    File file = new File(keystoreLocation);
                    log.debug("Loading keystore [location: {}]", file.getAbsolutePath());

                    try (FileInputStream stream = (file.exists() ? new FileInputStream(file) : null)) {
                        result = KeyStore.getInstance(KeyStore.getDefaultType());
                        result.load(stream, password.toCharArray());

                        keyStore = result;
                    }
                }
            }
        }

        return result;
    }

    public void saveKeyStore(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        synchronized (KeyStoreFactory.class) {
            File file = new File(keystoreLocation);
            log.debug("Saving keystore [location: {}]", file.getAbsolutePath());

            if (!file.exists()) {
                boolean newPath = file.getParentFile().mkdirs();
                log.debug("Creating keystore folder [new: {}]", newPath);

                boolean newFile = file.createNewFile();
                log.debug("Creating keystore file [new: {}]", newFile);
            }

            try (FileOutputStream stream = new FileOutputStream(file)) {
                keyStore.store(stream, password.toCharArray());
            }
        }
    }
}
