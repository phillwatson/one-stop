package com.hillayes.alphavantage.api;

import com.hillayes.shares.api.domain.ShareInfo;
import io.quarkus.test.junit.QuarkusTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@RequiredArgsConstructor
public class AlphaVantageProviderTest {
    private final AlphaVantageProvider fixture;

    @ParameterizedTest
    @ValueSource(strings = { "TW", "TW.", "TW.LON", "TW.LSE" })
    public void testGetShareInfo(String ticker) {
        Optional<ShareInfo> response = fixture.getShareInfo("isin", ticker);
        assertNotNull(response);

        assertTrue(response.isPresent());
        assertEquals("isin", response.get().getIsin());
        assertEquals("TW.LON", response.get().getTickerSymbol());
        assertEquals("Taylor Wimpey PLC", response.get().getName());
        assertEquals("GBP", response.get().getCurrency().getCurrencyCode());
    }

    @Test
    public void testGetShareInfo_NotFound() {
        Optional<ShareInfo> response = fixture.getShareInfo("isin", "ZZ.");
        assertNotNull(response);

        assertFalse(response.isPresent());
    }
}
