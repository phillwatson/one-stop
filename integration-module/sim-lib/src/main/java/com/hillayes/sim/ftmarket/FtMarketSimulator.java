package com.hillayes.sim.ftmarket;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.hillayes.sim.server.Expectation;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
public class FtMarketSimulator implements Closeable {
    private static final UrlPathPattern SUMMARY_URL_PATH = urlPathEqualTo("/data/funds/tearsheet/summary");
    private static final UrlPathPattern PRICES_URL_PATH = urlPathEqualTo("/data/equities/ajax/get-historical-prices");
    private static final StringValuePattern DATE_PATTERN = matching("^\\d{4}\\/\\d{2}\\/\\d{2}$");
    private final WireMock wireMockClient;

    public FtMarketSimulator(int wiremockPort) {
        log.info("Starting FundSummary Simulator [port: {}]", wiremockPort);
        wireMockClient = new WireMock(wiremockPort);
    }

    public void close() {
        wireMockClient.resetRequests();
        wireMockClient.removeMappings();
    }

    public Expectation expectSummaryFor(String symbol, String issueId, String name, String currency) {
        return new Expectation("fund summary for " + symbol, wireMockClient,
            get(SUMMARY_URL_PATH)
                .withHeader("Accept", containing(MediaType.TEXT_HTML))
                .withQueryParam("s", equalTo(symbol))
                .willReturn(aResponse()
                    .withHeader(ContentTypeHeader.KEY, MediaType.TEXT_HTML)
                    .withBody(loadPage())
                    .withTransformers("response-template")
                    .withTransformerParameter("symbol", symbol)
                    .withTransformerParameter("issueId", issueId)
                    .withTransformerParameter("name", name)
                    .withTransformerParameter("currency", currency)
                ).build(),

            getRequestedFor(SUMMARY_URL_PATH)
                .withHeader("Accept", containing(MediaType.TEXT_HTML))
                .withQueryParam("s", equalTo(symbol))
        );
    }


    public Expectation expectPricesFor(String symbol) {
        return new Expectation("history prices for " + symbol, wireMockClient,
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
        );
    }


    private static String SUMMARY_PAGE;
    private String loadPage() {
        if (SUMMARY_PAGE == null) {
            try {
                URL resource = this.getClass().getResource("/ftmarket/fund-summary.html");
                SUMMARY_PAGE = IOUtils.toString(resource, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return SUMMARY_PAGE;
    }
}
