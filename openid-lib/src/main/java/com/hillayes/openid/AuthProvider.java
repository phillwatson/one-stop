package com.hillayes.openid;

import java.net.URI;

/**
 * Identifies the Open-ID auth-provider to which this application has
 * been registered.
 */
public enum AuthProvider {
    GOOGLE("Google", URI.create("https://img.icons8.com/color/48/000000/google-logo.png")),
    GITHUB("GitHub", URI.create("https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png")),
    GITLAB("GitLab", URI.create("https://about.gitlab.com/images/press/press-kit-icon.svg")),
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
