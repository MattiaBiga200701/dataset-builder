package it.uniroma2.isw2.utils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** Codifica e decodifica del formato CSV. */
public final class CsvFormat {
    private CsvFormat() { }

    /** Parser CSV: gestisce celle quotate, virgolette raddoppiate e a capo nelle celle. */
    public static List<List<String>> parseCsv(String text) throws IOException {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        boolean pending = false;
        try (PushbackReader reader = new PushbackReader(new StringReader(text))) {
            int value;
            while ((value = reader.read()) != -1) {
                char c = (char) value;
                pending = true;
                if (quoted) {
                    if (c == '"') {
                        int next = reader.read();
                        if (next == '"') cell.append('"');
                        else {
                            quoted = false;
                            closedQuote = true;
                            if (next != -1) reader.unread(next);
                        }
                    } else cell.append(c);
                } else if (c == ',' || c == '\r' || c == '\n') {
                    row.add(cell.toString());
                    cell.setLength(0);
                    closedQuote = false;
                    if (c != ',') {
                        if (c == '\r') {
                            int next = reader.read();
                            if (next != '\n' && next != -1) reader.unread(next);
                        }
                        rows.add(row);
                        row = new ArrayList<>();
                        pending = false;
                    }
                } else if (closedQuote) {
                    throw new IOException("Carattere dopo la chiusura di una cella CSV");
                } else if (c == '"') {
                    if (!cell.isEmpty()) throw new IOException("Virgolette in una cella CSV non quotata");
                    quoted = true;
                } else cell.append(c);
            }
        }
        if (quoted) throw new IOException("Cella CSV con virgolette non chiuse");
        if (pending) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    public static String csvValue(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }

}
