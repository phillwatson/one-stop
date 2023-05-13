package com.hillayes.rail.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.auth.xsrf.XsrfInterceptor;
import io.quarkus.test.junit.mockito.InjectMock;

import javax.inject.Inject;

public abstract class TestBase {
    protected static final String adminIdStr = "0945990c-13d6-4aad-8b67-29291c9ba717";
    protected static final String userIdStr = "0945990c-13d6-4aad-8b67-29291c9ba716";

    @Inject
    ObjectMapper objectMapper;

    /**
     * Mocking the XsrfInterceptor to avoid the need to set the X-XSRF-TOKEN header in the tests.
     */
    @InjectMock
    XsrfInterceptor xsrfInterceptor;

    public String json(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
