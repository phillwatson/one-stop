package com.hillayes.user.openid.google;

import com.hillayes.user.openid.OpenIdConfiguration;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "one-stop.auth.openid.google")
public interface GoogleIdConfig extends OpenIdConfiguration {
}
