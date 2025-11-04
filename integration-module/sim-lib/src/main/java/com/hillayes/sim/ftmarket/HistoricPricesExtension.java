package com.hillayes.sim.ftmarket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.sim.server.SimExtension;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Singleton
public class HistoricPricesExtension implements SimExtension {
    public static final String NAME = "ftmarket-historic-prices";

    private static final RandomUtils RANDOM_NUMBERS = RandomUtils.insecure();
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);
    private static final DateTimeFormatter DATE_PARSER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter LONG_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("eeee, LLLL dd, uuuu");
    private static final ObjectMapper jsonMapper = MapperFactory.defaultMapper();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        Map<String, QueryParameter> queryParams = serveEvent.getRequest().getQueryParams();

        LocalDate startDate = LocalDate.from(DATE_PARSER.parse(queryParams.get("startDate").firstValue()));
        LocalDate endDate = LocalDate.from(DATE_PARSER.parse(queryParams.get("endDate").firstValue()));

        log.info("Generating historic prices [startDate: {}, endDate: {}]", startDate, endDate);
        return ResponseDefinitionBuilder.like(serveEvent.getResponseDefinition())
            .withHeader(ContentTypeHeader.KEY, MediaType.APPLICATION_JSON)
            .withStatus(200)
            .withBody(generatePrices(startDate, endDate))
            .build();
    }

    private String generatePrices(LocalDate startDate, LocalDate endDate) {
        StringBuilder result = new StringBuilder("{\"data\":{},\"html\":\"");

        LocalDate marketDate = endDate;
        while (!marketDate.isBefore(startDate)) {
            DayOfWeek dayOfWeek = marketDate.getDayOfWeek();
            if ((dayOfWeek != DayOfWeek.SATURDAY) && ((dayOfWeek != DayOfWeek.SUNDAY))) {
                result.append(ROW_TEMPLATE
                    .replace("{{market-date}}", LONG_DATE_FORMATTER.format(marketDate))
                    .replace("{{open}}", NUMBER_FORMAT.format(RANDOM_NUMBERS.randomDouble(100.00, 200.00)))
                    .replace("{{high}}", NUMBER_FORMAT.format(RANDOM_NUMBERS.randomDouble(100.00, 200.00)))
                    .replace("{{low}}", NUMBER_FORMAT.format(RANDOM_NUMBERS.randomDouble(100.00, 200.00)))
                    .replace("{{close}}", NUMBER_FORMAT.format(RANDOM_NUMBERS.randomDouble(100.00, 200.00)))
                    .replace("{{volume}}", NUMBER_FORMAT.format(RANDOM_NUMBERS.randomLong(1000000, 3000000)))
                );
            }

            marketDate = marketDate.minusDays(1);
        }

        result.append("\"}");
        return result.toString();
    }

    private static final String ROW_TEMPLATE =
        "\\u003ctr\\u003e" +
            "\\u003ctd class='mod-ui-table__cell--text'\\u003e" +
                "\\u003cspan class='mod-ui-hide-small-below'\\u003e{{market-date}}\\u003c/span\\u003e" +
                "\\u003cspan class='mod-ui-hide-medium-above'\\u003enot used\\u003c/span\\u003e\\u003c/td\\u003e" +
            "\\u003ctd\\u003e{{open}}\\u003c/td\\u003e" +
            "\\u003ctd\\u003e{{high}}\\u003c/td\\u003e" +
            "\\u003ctd\\u003e{{low}}\\u003c/td\\u003e" +
            "\\u003ctd\\u003e{{close}}\\u003c/td\\u003e" +
            "\\u003ctd\\u003e" +
                "\\u003cspan class='mod-ui-hide-small-below'\\u003e{{volume}}\\u003c/span\\u003e" +
                "\\u003cspan class='mod-ui-hide-medium-above'\\u003enot-used\\u003c/span\\u003e" +
            "\\u003c/td\\u003e" +
        "\\u003c/tr\\u003e";
}
