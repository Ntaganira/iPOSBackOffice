package com.ntaganira.heritier.ipos.backoffice.config;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("fmt")
public class ViewFormat {

    private final ZoneId zone;
    private final MessageSource messageSource;

    public ViewFormat(AppProperties properties, MessageSource messageSource) {
        this.zone = ZoneId.of(properties.zone());
        this.messageSource = messageSource;
    }

    public String money(Long cents) {
        long value = cents == null ? 0L : cents;
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale()));
        return "RWF " + format.format(BigDecimal.valueOf(value).movePointLeft(2));
    }

    public String amount(Long cents) {
        long value = cents == null ? 0L : cents;
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale()));
        return format.format(BigDecimal.valueOf(value).movePointLeft(2));
    }

    public String number(Long value) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(locale()));
        return format.format(value == null ? 0L : value);
    }

    public String number(Integer value) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(locale()));
        return format.format(value == null ? 0 : value);
    }

    public String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale()));
        return "RWF " + format.format(value == null ? BigDecimal.ZERO : value);
    }

    public String amount(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale()));
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    public String number(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(locale()));
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    public String qty(BigDecimal value) {
        return number(value);
    }

    public String percent(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale()));
        return format.format(value == null ? BigDecimal.ZERO : value) + "%";
    }

    public String localDate(LocalDate date) {
        return date == null ? "" : DateTimeFormatter.ofPattern("dd MMM yyyy", locale()).format(date);
    }

    public String date(LocalDate date) {
        return localDate(date);
    }

    public String dateInput(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    public String date(Instant instant) {
        return instant == null ? "" : formatter("dd MMM yyyy").format(instant);
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : formatter("dd MMM yyyy HH:mm").format(instant);
    }

    public String time(Instant instant) {
        return instant == null ? "" : formatter("HH:mm").format(instant);
    }

    public String monthDay(Instant instant) {
        return instant == null ? "" : formatter("dd MMM").format(instant);
    }

    public String isoDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    public String percentBps(Integer basisPoints) {
        int value = basisPoints == null ? 0 : basisPoints;
        return (value / 100.0) + "%";
    }

    public String msg(String key) {
        return messageSource.getMessage(key, null, key, locale());
    }

    public String initials(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(locale());
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase(locale());
    }

    public String shortId(String value) {
        if (value == null || value.length() <= 10) {
            return value == null ? "" : value;
        }
        return value.substring(0, 8);
    }

    private DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, locale()).withZone(zone);
    }

    private Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
