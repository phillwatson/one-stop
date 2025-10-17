package com.hillayes.shares.ft.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.Strings;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.ft.domain.IsinIssueLookup;
import com.hillayes.shares.ft.errors.ShareServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class MarketsClient {
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_PARSER =
        DateTimeFormatter.ofPattern("eeee, LLLL dd, uuuu");
    private static final TypeReference<Map<String,String>> MAP_TYPE_REFERENCE =
        new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    /**
     * Returns the issue-id by which the FT Finance API identifies companies and
     * funds.
     *
     * @param stockIsin the company, or fund, International Securities Identification Number
     * @return the FT Finance API issue ID for the given ISIN
     * @throws ShareServiceException
     */
    public Optional<IsinIssueLookup> getIssueID(String stockIsin) throws ShareServiceException {
        log.info("Retrieving stock issue-id [isin: {}]", stockIsin);
        try {
            // https://markets.ft.com/data/funds/tearsheet/summary?s=GB00B0CNGT73
            // Configure request with headers to avoid blocking
            Document doc = Jsoup.connect("https://markets.ft.com/data/funds/tearsheet/summary?s=" + stockIsin)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .header("Accept", "text/html")
                .header("Accept-Language", "en-GB,en;q=0.5")
                .get();

            String issueId = extractIssueId(doc);
            if (issueId == null) {
                return Optional.empty();
            }

            return Optional.of(IsinIssueLookup.builder()
                .isin(stockIsin)
                .issueId(issueId)
                .name(extractName(doc))
                .currencyCode(extractCurrencyCode(doc))
                .build()
            );
        } catch (IOException e) {
            throw new ShareServiceException("IsinLookupService", e, Map.of("isin", stockIsin));
        }
    }

    private String extractIssueId(Document doc) throws JsonProcessingException {
        String result = null;
        Element input = doc.selectFirst("input[name=issueID]");
        if (input == null) {
            Element li = doc.selectFirst("li.mod-news__mind-event[data-mod-mind*=xid]");
            if (li != null) {
                Map<String,String> json = objectMapper.readValue(li.attribute("data-mod-mind").getValue(), MAP_TYPE_REFERENCE);
                result = json.get("xid");
            }
        } else {
            Attribute valueAttr = input.attribute("value");
            result = valueAttr.getValue();
        }

        return result;
    }

    private String extractName(Document doc) {
        String result = doc.title();
        if (Strings.isBlank(result)) {
            return null;
        }
        return result.split(",")[0];
    }

    private String extractCurrencyCode(Document doc) {
        String result = null;
        Element input = doc.selectFirst("input[name=currencyCode]");
        if (input != null) {
            result = input.attribute("value").getValue();
        }

        if (result == null) {
            Elements profileRows = doc.select("table.mod-profile-and-investment-app__table--profile tr");
            result = profileRows.stream()
                .filter(row -> row.firstElementChild().hasText())
                .filter(row -> row.firstElementChild().text().toLowerCase().contains("currency"))
                .findFirst()
                .map(row -> row.child(1).text())
                .orElse(null);
        }

        return result;
    }

    public List<PriceData> getPrices(String issueId, LocalDate startDate, LocalDate endDate) {
        log.info("Retrieving share prices [issueId: {}, startDate: {}, endDate: {}]", issueId, startDate, endDate);
        try {
            //https://markets.ft.com/data/equities/ajax/get-historical-prices?startDate=2025/08/28&endDate=2025/09/28&symbol=74137468
            String url = "https://markets.ft.com/data/equities/ajax/get-historical-prices" +
                "?startDate=" + startDate.format(DATE_FORMATTER) +
                "&endDate=" + endDate.format(DATE_FORMATTER) +
                "&symbol=" + issueId;

            String doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .ignoreContentType(true)
                .header("Accept", "application/json; charset=utf-8")
                .header("Accept-Language", "en-GB,en;q=0.5")
                .get()
                .body().text();

            JsonNode node = objectMapper.readTree(doc);
            JsonNode xml = node.findPath("html");

            Document data = Parser.xmlParser().parseInput(xml.asText(), "http://localhost");
            List<PriceData> result = data.getElementsByTag("tr").stream()
                .map(row -> {
                    Elements cols = row.getElementsByTag("td");
                    String date = cols.get(0).getElementsByClass("mod-ui-hide-small-below").get(0).text();

                    return new PriceData(
                        LocalDate.parse(date, DATE_PARSER),
                        BigDecimal.valueOf(Double.parseDouble(cols.get(1).text().replace(",", ""))),
                        BigDecimal.valueOf(Double.parseDouble(cols.get(2).text().replace(",", ""))),
                        BigDecimal.valueOf(Double.parseDouble(cols.get(3).text().replace(",", ""))),
                        BigDecimal.valueOf(Double.parseDouble(cols.get(4).text().replace(",", "")))
                    );
                })
                .sorted(Comparator.comparing(PriceData::date))
                .toList();

            log.debug("Retrieved share prices [issueId: {}, startDate: {}, endDate: {}, size: {}]", issueId, startDate, endDate, result.size());
            return result;
        } catch (IOException e) {
            throw new ShareServiceException("PriceLookupService", e,
                Map.of(
                    "issueId", issueId,
                    "startDate", startDate,
                    "endDate", endDate
                ));
        }
    }
}
