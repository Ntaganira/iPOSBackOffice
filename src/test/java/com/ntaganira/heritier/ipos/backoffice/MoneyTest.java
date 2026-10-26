package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : MoneyTest.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Unit tests for monetary calculations.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void taxIsExtractedFromVatInclusiveGross() {
        assertThat(Money.taxFromGross(new BigDecimal("11800"), new BigDecimal("18")))
                .isEqualByComparingTo("1800.00");
        assertThat(Money.taxFromGross(new BigDecimal("1000"), BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void moneyRoundsToTwoDecimals() {
        assertThat(Money.of("10.555")).isEqualByComparingTo("10.56");
        assertThat(Money.of((BigDecimal) null)).isEqualByComparingTo("0.00");
    }
}
