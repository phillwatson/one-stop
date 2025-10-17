package com.hillayes.shares.config;

import com.hillayes.shares.api.ShareProviderApi;
import com.hillayes.shares.api.domain.ShareProvider;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    public void test(ShareProvider providerId) {
        // When:
        ShareProviderApi shareProviderApi = shareProviderFactory.get(providerId);

        // Then:
        assertNotNull(shareProviderApi);

        // And:
        assertEquals(providerId, shareProviderApi.getProviderId());
    }
}
