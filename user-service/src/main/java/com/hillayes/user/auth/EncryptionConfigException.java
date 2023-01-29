package com.hillayes.user.auth;

import java.security.GeneralSecurityException;

public class EncryptionConfigException extends AuthException {
    public EncryptionConfigException(GeneralSecurityException aCause) {
        super(AuthErrorCodes.ENCRYPTION_CONFIG, aCause);
    }
}
