package com.say5.livevents.api;

/**
 * Minimal RFC 4180 CSV escaping. Quotes a field if it contains a comma, a
 * double-quote, a newline, or a carriage return; doubles any embedded quote.
 */
public final class CsvEscape {

    private CsvEscape() {
    }

    public static String row(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    public static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.indexOf(',') >= 0
                || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0
                || field.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }
}
