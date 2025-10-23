package com.hillayes.shares.config;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.errors.ProviderNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@RequiredArgsConstructor
public class ShareProviderFactoryTest {
    private final ShareProviderFactory shareProviderFactory;

    @Test
    public void testGetAll() {
        assertEquals(ShareProvider.values().length, shareProviderFactory.getAll().toList().size());
    }

    @ParameterizedTest
    @EnumSource(ShareProvider.class)
    public void testGet_Success(ShareProvider providerId) {
        // When:
        ShareProviderApi shareProviderApi = shareProviderFactory.get(providerId);

        // Then:
        assertNotNull(shareProviderApi);

        // And:
        assertEquals(providerId, shareProviderApi.getProviderId());
    }

    @ParameterizedTest
    @EnumSource(ShareProvider.class)
    public void testGetImlementation_Success(ShareProvider providerId) {
        // When:
        ShareProviderApi shareProviderApi = shareProviderFactory.getImplementation(providerId.name());

        // Then:
        assertNotNull(shareProviderApi);

        // And:
        assertEquals(providerId, shareProviderApi.getProviderId());
    }

    @Test
    public void testGetImplementation_NotFound() {
        // When:
        ProviderNotFoundException expected = assertThrows(ProviderNotFoundException.class, () ->
            shareProviderFactory.getImplementation("invalid-provider")
        );

        // Then: the exception identifies the given parameter
        assertEquals("invalid-provider", expected.getParameter("shares-provider"));
    }
}
