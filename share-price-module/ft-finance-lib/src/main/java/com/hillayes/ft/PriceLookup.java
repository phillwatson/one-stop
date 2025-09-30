package com.hillayes.ft;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PriceLookup {
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATE_PARSER =
        DateTimeFormatter.ofPattern("eeee, LLLL dd, uuuu");

    private Optional<String> getIssueID(String isin) throws IOException {
        // https://markets.ft.com/data/funds/tearsheet/historical?s=GB00B0CNGT73:GBP
        // Configure request with headers to avoid blocking
        Document doc = Jsoup.connect("https://markets.ft.com/data/funds/tearsheet/historical?s=" + isin)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
            .header("Accept", "text/html")
            .header("Accept-Language", "en-US")
            .get();

        Element input = doc.selectFirst("input[name=issueID]");
        if (input == null) {
            return Optional.empty();
        }

        Attribute value = input.attribute("value");
        return Optional.ofNullable(value == null ? null : value.getValue());
    }

    private List<PriceData> getPrices(String issueId, LocalDate startDate, LocalDate endDate) throws IOException {
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
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .get()
            .body().text();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(doc);
        JsonNode xml = node.findPath("html");

        Document data = Parser.xmlParser().parseInput(xml.asText(), "http://localhost");
        List<PriceData> result = data.getElementsByTag("tr").stream().map(row -> {
                Elements cols = row.getElementsByTag("td");
                String date = cols.get(0).getElementsByClass("mod-ui-hide-small-below").get(0).text();
                String value = cols.get(4).text().replace(",", "");

                return new PriceData(
                    LocalDate.parse(date, DATE_PARSER),
                    Double.parseDouble(value)
                );
            }
        ).toList();
        return result;
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        PriceLookup lookup = new PriceLookup();
        lookup.getIssueID("GB00B0CNGT73:GBP")
            .ifPresent(issueId -> {
                System.out.println(issueId);
                try {
                    List<PriceData> prices = lookup.getPrices(issueId, LocalDate.now().minusDays(20), LocalDate.now());
                    System.out.println(prices);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public record PriceData(
        LocalDate date,
        Double price
    ) {}
}
