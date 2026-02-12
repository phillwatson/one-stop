package com.hillayes.ftmarket.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.Strings;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.ftmarket.api.domain.CurrencyUnits;
import com.hillayes.ftmarket.api.domain.IsinIssueLookup;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.api.domain.ShareProvider;
import com.hillayes.shares.api.errors.ShareServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
@Slf4j
public class MarketsClient {
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_PARSER =
        DateTimeFormatter.ofPattern("eeee, LLLL dd, uuuu");
    private static final TypeReference<Map<String,String>> MAP_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final Pattern PRICE_CURRENCY = Pattern.compile("^Price \\((.*)\\)");

    private static final ObjectMapper OBJECT_MAPPER = MapperFactory.defaultMapper();
    private final String host;

    public MarketsClient(@ConfigProperty(name = "one-stop.shares.ft-market.url") String host) {
        this.host = host;
        log.debug("Creating client [host: {}]", host);
    }

    /**
     * Returns the issue-id by which the FT Finance API identifies companies and
     * funds.
     *
     * @param symbol the ticker symbol or ISIN (International Securities Identification Number)
     * @return the FT Finance API issue ID for the given symbol
     * @throws ShareServiceException
     */
    public Optional<IsinIssueLookup> getIssueID(String symbol) throws ShareServiceException {
        log.info("Retrieving stock issue-id [symbol: {}]", symbol);
        try {
            // https://markets.ft.com/data/funds/tearsheet/summary?s=GB00B0CNGT73
            // Configure request with headers to avoid blocking
            Document doc = Jsoup.connect(host + "/data/funds/tearsheet/summary?s=" + symbol)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .header("Accept", "text/html")
                .header("Accept-Language", "en-GB,en;q=0.5")
                .get();

            String issueId = extractIssueId(doc);
            if (issueId == null) {
                return Optional.empty();
            }

            String currencyCode = extractCurrencyCode(doc);
            CurrencyUnits currencyUnits = CurrencyUnits.MAJOR;
            if ((Strings.isBlank(currencyCode)) || ("GBX".equals(currencyCode))) {
                currencyCode = "GBP";
                currencyUnits = CurrencyUnits.MINOR;
            }
            return Optional.of(IsinIssueLookup.builder()
                .isin(symbol)
                .issueId(issueId)
                .name(extractName(doc))
                .currencyCode(currencyCode)
                .currencyUnits(currencyUnits)
                .build()
            );
        } catch (IOException e) {
            throw new ShareServiceException(ShareProvider.FT_MARKET_DATA, "IsinLookupService", e, Map.of("isin", symbol));
        }
    }

    private String extractIssueId(Document doc) throws JsonProcessingException {
        String result = null;
        Element input = doc.selectFirst("input[name=issueID]");
        if (input != null) {
            Attribute valueAttr = input.attribute("value");
            if (valueAttr != null) {
                result = valueAttr.getValue();
            }
        }

        if (result == null) {
            Attribute attribute = null;
            Element element = doc.selectFirst("li.mod-news__mind-event[data-mod-mind*=xid]");
            if (element != null) {
                attribute = element.attribute("data-mod-mind");
            }
            else {
                element = doc.selectFirst("section[data-mod-config*=xid]");
                if (element != null) {
                    attribute = element.attribute("data-mod-config");
                }
            }

            if (attribute != null) {
                Map<String, String> json = OBJECT_MAPPER.readValue(attribute.getValue(), MAP_TYPE_REFERENCE);
                result = json.get("xid");
            }
        }

        return result;
    }

    private String extractName(Document doc) {
        String result = doc.title();
        return Strings.isBlank(result) ? null : result.split(",")[0];
    }

    private String extractCurrencyCode(Document doc) {
        String result = null;

        Element price = doc.selectFirst("ul.mod-tearsheet-overview__quote__bar");
        if (price != null) {
            result = price.children().stream()
                .map(Element::text)
                .filter(text -> text.toLowerCase().contains("price ("))
                .findAny()
                .map(PRICE_CURRENCY::matcher)
                .filter(m -> m.find() && m.groupCount() > 0)
                .map(m -> m.group(1))
                .orElse(null);
        }
//
//        if (result == null) {
//            Element input = doc.selectFirst("input[name=currencyCode]");
//            if (input != null) {
//                Attribute attribute = input.attribute("value");
//                if (attribute != null) {
//                    result = attribute.getValue();
//                }
//            }
//        }
//
//        if (result == null) {
//            Elements profileRows = doc.select("table.mod-profile-and-investment-app__table--profile tr");
//            result = profileRows.stream()
//                .filter(row -> row.firstElementChild() != null)
//                .filter(row -> row.firstElementChild().hasText())
//                .filter(row -> row.firstElementChild().text().toLowerCase().contains("currency"))
//                .map(row -> row.child(1).text())
//                .findFirst().orElse(null);
//        }

        return result;
    }

    public List<PriceData> getPrices(String issueId, CurrencyUnits currencyUnits, LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving share prices [issueId: {}, startDate: {}, endDate: {}]", issueId, startDate, endDate);
        try {
            //https://markets.ft.com/data/equities/ajax/get-historical-prices?startDate=2025/08/28&endDate=2025/09/28&symbol=74137468
            String url = host + "/data/equities/ajax/get-historical-prices" +
                "?startDate=" + startDate.format(DATE_FORMATTER) +
                "&endDate=" + endDate.format(DATE_FORMATTER) +
                "&symbol=" + issueId;

            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .ignoreContentType(true)
                .header("Accept", "application/json; charset=utf-8")
                .header("Accept-Language", "en-GB,en;q=0.5")
                .get();

            JsonNode node = OBJECT_MAPPER.readTree(doc.body().text());
            JsonNode xml = node.findPath("html");

            Document data = Parser.xmlParser().parseInput(xml.asText(), "http://localhost"); // any URL will do
            List<PriceData> result = data.getElementsByTag("tr").stream()
                .map(row -> {
                    Elements cols = row.getElementsByTag("td");
                    String date = cols.get(0).getElementsByClass("mod-ui-hide-small-below").get(0).text();
                    String volume = cols.get(5).getElementsByClass("mod-ui-hide-small-below").get(0).text();

                    return new PriceData(
                        LocalDate.parse(date, DATE_PARSER),
                        parsePrice(cols.get(1).text(), currencyUnits),
                        parsePrice(cols.get(2).text(), currencyUnits),
                        parsePrice(cols.get(3).text(), currencyUnits),
                        parsePrice(cols.get(4).text(), currencyUnits),
                        Long.valueOf(volume.replace(",", ""))
                    );
                })
                .sorted(Comparator.comparing(PriceData::date))
                .toList();

            log.debug("Retrieved share prices [issueId: {}, startDate: {}, endDate: {}, size: {}]", issueId, startDate, endDate, result.size());
            return result;
        } catch (IOException e) {
            throw new ShareServiceException(ShareProvider.FT_MARKET_DATA, "PriceLookupService", e,
                Map.of(
                    "issueId", issueId,
                    "startDate", startDate,
                    "endDate", endDate
                ));
        }
    }

    BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private BigDecimal parsePrice(String text, CurrencyUnits units) {
        BigDecimal value = BigDecimal.valueOf(Double.parseDouble(text.replace(",", "")));

        // ensure price is recorded in minor units
        return (units == CurrencyUnits.MAJOR) ? value.multiply(ONE_HUNDRED) : value;
    }
}
