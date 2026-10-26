package com.ntaganira.heritier.ipos.backoffice.util;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.util
 * - File      : Money.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Monetary arithmetic and rounding helpers.
 * </pre>
 */

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? zero() : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        if (value == null || value.isBlank()) {
            return zero();
        }
        return new BigDecimal(value.trim()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return of((a == null ? BigDecimal.ZERO : a).multiply(b == null ? BigDecimal.ZERO : b));
    }

    public static BigDecimal taxFromGross(BigDecimal gross, BigDecimal ratePercent) {
        if (gross == null || ratePercent == null || ratePercent.signum() <= 0) {
            return zero();
        }
        return gross.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100).add(ratePercent), SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    public static BigDecimal quantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(3, RoundingMode.HALF_UP);
    }

    public static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
