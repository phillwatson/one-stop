package com.hillayes.rail.shares;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

/**
 * ISIN
 * GB00B5ZNJ896:GBP - BlackRock Gold and General D Acc
 * GB0033874321:GBP - FSSA Greater China Growth B GBP Acc
 * GB00BP9LKJ73:GBP - FTF Royce US Smaller Companies W Acc
 * GB00B80QG052:GBP - HSBC FTSE 250 Index C Acc
 * GB00B0CNGR59:GBP - L&G European Index I Acc
 * GB00B0CNGT73:GBP - L&G US Index I Acc
 * IE00B3NS4D25:GBP - Lindsell Train Global Equity B GBP Inc
 * AV.
 * TW.
 * NG.
 * LGEN
 * PSN
 */
public class FundPriceRepository {
    private static final String FT_URI = "https://markets.ft.com/data/funds/tearsheet";
    private static final String FUND = "GB00BF18C781:GBP";

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("EEEE, LLLL dd, yyyy")
        .toFormatter();

    public List<DailyPrice> readPrices(String fund) throws Exception {
        return parseHtml(getClient().getHistoricalPrices(fund));
    }

    private List<DailyPrice> parseHtml(String html) throws Exception {
        List<List<String>> rowCols = HtmlScraper.scrape(html);

        return rowCols.stream().map(row ->
            new DailyPrice(
                LocalDate.parse(row.get(0), DATE_FORMATTER),
                Float.parseFloat(row.get(2)),
                Float.parseFloat(row.get(3)),
                Float.parseFloat(row.get(4)),
                Float.parseFloat(row.get(5)))
        ).toList();
    }

    private FinancialTimesApi getClient() {
        URI baseUri = URI.create(FT_URI);
        return RestClientBuilder.newBuilder()
            .baseUri(baseUri)
            .build(FinancialTimesApi.class);
    }

    public static void main(String[] args) throws Exception {
        new FundPriceRepository().readPrices(FUND).forEach(System.out::println);
    }
}
