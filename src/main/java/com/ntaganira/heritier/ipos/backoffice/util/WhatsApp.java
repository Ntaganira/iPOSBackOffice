package com.ntaganira.heritier.ipos.backoffice.util;

import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WhatsApp {

    private WhatsApp() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        boolean hasCountryCode = value.startsWith("+") || value.startsWith("00");
        String digits = value.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (!hasCountryCode) {
            if (digits.length() == 10 && digits.startsWith("0")) {
                digits = "250" + digits.substring(1);
            } else if (digits.length() == 9 && digits.startsWith("7")) {
                digits = "250" + digits;
            }
        }
        if (digits.length() < 8 || digits.length() > 15) {
            throw new IllegalArgumentException(
                    "WhatsApp number must include the country code, for example +250 7XX XXX XXX.");
        }
        return "+" + digits;
    }

    public static String tryNormalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return normalize(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String link(String normalizedNumber, String message) {
        String recipient = normalizedNumber == null ? "" : normalizedNumber.replaceAll("\\D", "");
        return "https://wa.me/" + recipient + "?text=" + encode(message);
    }

    private static String encode(String message) {
        return URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
