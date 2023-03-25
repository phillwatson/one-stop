package com.hillayes.openid;

/**
 * Identifies the Open-ID auth-provider to which this application has
 * been registered.
 */
public enum AuthProvider {
    GOOGLE,
    APPLE;

    public static AuthProvider id(String name) {
        if ((name != null) && (!name.isBlank())) {
            try {
                return AuthProvider.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignore) {}
        }

        return null;
    }
}
