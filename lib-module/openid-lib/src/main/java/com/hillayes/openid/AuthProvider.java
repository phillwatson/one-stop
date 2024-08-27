package com.hillayes.openid;

import java.net.URI;

/**
 * Identifies the Open-ID auth-provider to which this application has
 * been registered.
 */
public enum AuthProvider {
    GOOGLE("Google", URI.create("/openid/google.png")),
    GITHUB("GitHub", URI.create("/openid/github.png")),
    GITLAB("GitLab", URI.create("/openid/gitlab.svg")),
    LINKEDIN("LinkedIn", URI.create("/openid/linkedin.png")),
    APPLE("Apple", URI.create(""));

    private final String provider;
    private final URI logo;

    AuthProvider(String name, URI logo) {
        this.provider = name;
        this.logo = logo;
    }

    public String getProviderName() {
        return provider;
    }

    public URI getLogo() {
        return logo;
    }

    public static AuthProvider id(String name) {
        if ((name != null) && (!name.isBlank())) {
            try {
                return AuthProvider.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignore) {}
        }

        return null;
    }
}
