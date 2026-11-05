package com.ntaganira.heritier.ipos.backoffice.util;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.util
 * - File      : ThemePalette.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Computes contrast-safe text colors for an arbitrary shop palette.
 * </pre>
 */

import java.util.Locale;
import java.util.regex.Pattern;

public record ThemePalette(
        String primary,
        String secondary,
        String background,
        String surface,
        String backgroundInk,
        String backgroundMuted,
        String surfaceInk,
        String surfaceMuted,
        String surfaceSubtle,
        String primaryInk,
        String secondaryInk,
        String brandInk,
        String backgroundAccent,
        String surfaceAccent
) {

    private static final String DARK_INK = "#0B1220";
    private static final String LIGHT_INK = "#FFFFFF";
    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");

    public static ThemePalette from(String primary, String secondary, String background, String surface) {
        String primaryColor = normalize(primary, "#268071");
        String secondaryColor = normalize(secondary, "#4FB8A8");
        String backgroundColor = normalize(background, "#FFFFFF");
        String surfaceColor = normalize(surface, "#FFFFFF");

        String backgroundInk = bestInk(backgroundColor);
        String surfaceInk = bestInk(surfaceColor);

        return new ThemePalette(
                primaryColor,
                secondaryColor,
                backgroundColor,
                surfaceColor,
                backgroundInk,
                readableTone(backgroundColor, backgroundInk, 4.6, 0.60),
                surfaceInk,
                readableTone(surfaceColor, surfaceInk, 4.6, 0.60),
                readableTone(surfaceColor, surfaceInk, 3.2, 0.42),
                bestInk(primaryColor),
                bestInk(secondaryColor),
                brandInk(primaryColor, secondaryColor),
                readableAccent(backgroundColor, primaryColor, backgroundInk, 4.6),
                readableAccent(surfaceColor, primaryColor, surfaceInk, 4.8)
        );
    }

    public static double contrast(String first, String second) {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    static String bestInk(String background) {
        double darkContrast = contrast(DARK_INK, background);
        double lightContrast = contrast(LIGHT_INK, background);
        return darkContrast >= lightContrast ? DARK_INK : LIGHT_INK;
    }

    static String readableTone(String base, String ink, double target, double startWeight) {
        for (double weight = startWeight; weight <= 1.0; weight += 0.02) {
            String candidate = blend(ink, base, weight);
            if (contrast(candidate, base) >= target) {
                return candidate;
            }
        }
        return ink;
    }

    static String readableAccent(String base, String accent, String ink, double target) {
        if (contrast(accent, base) >= target) {
            return accent;
        }
        for (double weight = 0.94; weight >= 0.10; weight -= 0.04) {
            String candidate = blend(accent, ink, weight);
            if (contrast(candidate, base) >= target) {
                return candidate;
            }
        }
        return ink;
    }

    private static String brandInk(String primary, String secondary) {
        double darkScore = Math.min(contrast(DARK_INK, primary), contrast(DARK_INK, secondary));
        double lightScore = Math.min(contrast(LIGHT_INK, primary), contrast(LIGHT_INK, secondary));
        return darkScore >= lightScore ? DARK_INK : LIGHT_INK;
    }

    private static String blend(String first, String second, double firstWeight) {
        int[] a = channels(first);
        int[] b = channels(second);
        int red = clamp(a[0] * firstWeight + b[0] * (1 - firstWeight));
        int green = clamp(a[1] * firstWeight + b[1] * (1 - firstWeight));
        int blue = clamp(a[2] * firstWeight + b[2] * (1 - firstWeight));
        return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
    }

    private static double luminance(String hex) {
        int[] rgb = channels(hex);
        return 0.2126 * linear(rgb[0]) + 0.7152 * linear(rgb[1]) + 0.0722 * linear(rgb[2]);
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int[] channels(String hex) {
        String normalized = normalize(hex, "#000000").substring(1);
        return new int[]{
                Integer.parseInt(normalized.substring(0, 2), 16),
                Integer.parseInt(normalized.substring(2, 4), 16),
                Integer.parseInt(normalized.substring(4, 6), 16)
        };
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String candidate = value.trim().toUpperCase(Locale.ROOT);
        return HEX.matcher(candidate).matches() ? candidate : fallback;
    }
}
