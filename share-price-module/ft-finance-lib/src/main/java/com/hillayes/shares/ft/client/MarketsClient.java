package com.hillayes.shares.ft.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.shares.api.domain.PriceData;
import com.hillayes.shares.ft.errors.ShareServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class MarketsClient {
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_PARSER =
        DateTimeFormatter.ofPattern("eeee, LLLL dd, uuuu");

    /**
     * Returns the issue-id by which the FT Finance API identifies companies and
     * funds.
     *
     * @param stockIsin the company, or fund, International Securities Identification Number
     * @return the FT Finance API issue ID for the given ISIN
     * @throws ShareServiceException
     */
    public Optional<String> getIssueID(String stockIsin) throws ShareServiceException {
        log.info("Retrieving stock issue-id [isin: {}]", stockIsin);
        try {
            // https://markets.ft.com/data/funds/tearsheet/historical?s=GB00B0CNGT73:GBP
            // Configure request with headers to avoid blocking
            Document doc = Jsoup.connect("https://markets.ft.com/data/funds/tearsheet/historical?s=" + stockIsin)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .header("Accept", "text/html")
                .header("Accept-Language", "en-GB,en;q=0.5")
                .get();

            Element input = doc.selectFirst("input[name=issueID]");
            if (input == null) {
                return Optional.empty();
            }

            Attribute value = input.attribute("value");
            return Optional.ofNullable(value == null ? null : value.getValue());
        } catch (IOException e) {
            throw new ShareServiceException("IsinLookupService", e, Map.of("isin", stockIsin));
        }
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

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(doc);
            JsonNode xml = node.findPath("html");

            Document data = Parser.xmlParser().parseInput(xml.asText(), "http://localhost");
            List<PriceData> result = data.getElementsByTag("tr").stream()
                .map(row -> {
                    Elements cols = row.getElementsByTag("td");
                    String date = cols.get(0).getElementsByClass("mod-ui-hide-small-below").get(0).text();

                    return new PriceData(
                        LocalDate.parse(date, DATE_PARSER),
                        Float.parseFloat(cols.get(1).text().replace(",", "")),
                        Float.parseFloat(cols.get(2).text().replace(",", "")),
                        Float.parseFloat(cols.get(3).text().replace(",", "")),
                        Float.parseFloat(cols.get(4).text().replace(",", ""))
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
