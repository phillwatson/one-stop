package com.hillayes.rail.resource;

import com.hillayes.auth.xsrf.XsrfInterceptor;
import com.hillayes.onestop.api.ErrorSeverity;
import com.hillayes.onestop.api.ServiceError;
import com.hillayes.onestop.api.ServiceErrorResponse;
import io.quarkus.test.InjectMock;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestBase {
    protected static final String userIdStr = "0945990c-13d6-4aad-8b67-29291c9ba716";

    /**
     * Mocking the XsrfInterceptor to avoid the need to set the X-XSRF-TOKEN header in the tests.
     */
    @InjectMock
    XsrfInterceptor xsrfInterceptor;

    protected void assertNotFoundError(ServiceErrorResponse response,
                                       Consumer<Map<String, String>> contextAttributesConsumer) {
        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());

        ServiceError error = response.getErrors().get(0);
        assertEquals(ErrorSeverity.INFO, error.getSeverity());
        assertEquals("ENTITY_NOT_FOUND", error.getMessageId());
        assertEquals("The identified entity cannot be found.", error.getMessage());

        // and: the response contains context attributes
        Map<String, String> contextAttributes = error.getContextAttributes();
        assertNotNull(contextAttributes);
        assertFalse(contextAttributes.isEmpty());

        if (contextAttributesConsumer != null) {
            // pass the context attributes to the consumer for assertions
            contextAttributesConsumer.accept(contextAttributes);
        }
    }
}
