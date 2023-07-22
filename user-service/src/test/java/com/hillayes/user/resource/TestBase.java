package com.hillayes.user.resource;

import com.hillayes.auth.xsrf.XsrfInterceptor;
import io.quarkus.test.InjectMock;

public abstract class TestBase {
    protected static final String adminIdStr = "0945990c-13d6-4aad-8b67-29291c9ba717";
    protected static final String userIdStr = "0945990c-13d6-4aad-8b67-29291c9ba716";

    /**
     * Mocking the XsrfInterceptor to avoid the need to set the X-XSRF-TOKEN header in the tests.
     */
    @InjectMock
    XsrfInterceptor xsrfInterceptor;
}
