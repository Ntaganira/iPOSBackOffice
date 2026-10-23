package com.ntaganira.heritier.ipos.backoffice.util;

import java.util.ArrayList;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    public static String line(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escape(values[i]));
        }
        return builder.append("\r\n").toString();
    }

    public static List<String> parse(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    public static String sanitizeFormula(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.stripLeading();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-")
                || trimmed.startsWith("@") || trimmed.startsWith("\t") || trimmed.startsWith("\r")) {
            return "'" + value;
        }
        return value;
    }
}
