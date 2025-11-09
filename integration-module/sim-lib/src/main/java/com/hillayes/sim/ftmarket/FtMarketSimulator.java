package com.hillayes.sim.ftmarket;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.hillayes.sim.server.Expectation;
import com.hillayes.sim.server.FileCache;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
public class FtMarketSimulator implements Closeable {
    // a base path to ensure ft-market calls can be differentiated from other mocked services
    public static final String BASE_URI = "/api.ft-market";

    // various patterns used within the mocked requests
    private static final UrlPathPattern SUMMARY_URL_PATH = urlPathEqualTo(BASE_URI + "/data/funds/tearsheet/summary");
    private static final UrlPathPattern PRICES_URL_PATH = urlPathEqualTo(BASE_URI + "/data/equities/ajax/get-historical-prices");
    private static final StringValuePattern DATE_PATTERN = matching("^\\d{4}\\/\\d{2}\\/\\d{2}$");

    private final WireMock wireMockClient;

    public FtMarketSimulator(int wiremockPort) {
        log.info("Starting FT Market Simulator [port: {}]", wiremockPort);
        wireMockClient = new WireMock(wiremockPort);
    }

    public void close() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();
    }

    public void expectSummaryFor(String symbol, String issueId,
                                 Consumer<Expectation> action) {
        try (Expectation expectation = new Expectation("fund summary for " + symbol, wireMockClient,
            get(SUMMARY_URL_PATH)
                .withHeader("Accept", containing(MediaType.TEXT_HTML))
                .withQueryParam("s", equalTo(symbol))
                .willReturn(aResponse()
                    .withHeader(ContentTypeHeader.KEY, MediaType.TEXT_HTML)
                    .withBody(FileCache.loadFile("/ftmarket/fund-summary.html"))
                    .withTransformers("response-template")
                    .withTransformerParameter("symbol", symbol)
                    .withTransformerParameter("issueId", issueId)
                    .withTransformerParameter("name", "company " + symbol)
                    .withTransformerParameter("currency", "GBP")
                ).build(),

            getRequestedFor(SUMMARY_URL_PATH)
                .withHeader("Accept", containing(MediaType.TEXT_HTML))
                .withQueryParam("s", equalTo(symbol))
        )) {
            action.accept(expectation);
        }
    }

    public void expectPricesFor(String symbol, Consumer<Expectation> action) {
        try (Expectation expectation = new Expectation("historic prices for " + symbol, wireMockClient,
            get(PRICES_URL_PATH)
                .withHeader("Accept", containing(MediaType.APPLICATION_JSON))
                .withQueryParam("startDate", DATE_PATTERN)
                .withQueryParam("endDate", DATE_PATTERN)
                .withQueryParam("symbol", equalTo(symbol))
                .willReturn(aResponse()
                    .withHeader(ContentTypeHeader.KEY, MediaType.APPLICATION_JSON)
                    .withTransformers(HistoricPricesExtension.NAME)
                ).build(),

            getRequestedFor(PRICES_URL_PATH)
                .withHeader("Accept", containing(MediaType.APPLICATION_JSON))
                .withQueryParam("startDate", DATE_PATTERN)
                .withQueryParam("endDate", DATE_PATTERN)
                .withQueryParam("symbol", equalTo(symbol))
        )) {
            action.accept(expectation);
        }
    }
}
