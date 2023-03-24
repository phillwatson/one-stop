package com.hillayes.user.oauth;

/**
 * Identifies the OpenID auth-provider.
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
