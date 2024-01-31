package com.hillayes.rail.shares;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class HtmlScraper extends HTMLEditorKit.ParserCallback {
    private static final ParserDelegator DELEGATOR = new ParserDelegator();

    public static List<List<String>> scrape(String html) throws java.lang.Exception {
        int tableStart = html.indexOf("mod-ui-table mod-tearsheet-historical-prices__results");
        int tableEnd = html.indexOf("</table>", tableStart);
        String table = html.substring(tableStart, tableEnd);

        int bodyStart = table.indexOf("<tbody>");
        int bodyEnd = table.indexOf("</tbody>", bodyStart);
        String rows = table.substring(bodyStart, bodyEnd);

        HtmlScraper scraper = new HtmlScraper();
        try (Reader reader = new BufferedReader(new CharArrayReader(rows.toCharArray()))) {
            DELEGATOR.parse(reader, scraper, false);
        }
        return scraper.rows;
    }

    private boolean inTD = false;
    private final List<List<String>> rows = new ArrayList<>();
    private List<String> currentRow;

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t.equals(HTML.Tag.TR)) {
            currentRow = new ArrayList<>();
        } else if (t.equals(HTML.Tag.TD)) {
            inTD = true;
        }
    }

    public void handleEndTag(HTML.Tag t, int pos) {
        if (t.equals(HTML.Tag.TD)) {
            inTD = false;
        } else if (t.equals(HTML.Tag.TR)) {
            rows.add(currentRow);
            currentRow = null;
        }
    }

    public void handleText(char[] data, int pos) {
        if (inTD) {
            currentRow.add(new String(data));
        }
    }
}
