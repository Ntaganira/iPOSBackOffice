package com.ntaganira.heritier.ipos.backoffice.entity;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.entity
 * - File      : StockPeriodLine.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : JPA entity representing stock period line.
 * </pre>
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_period_lines")
public class StockPeriodLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private StockPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "opening_qty", nullable = false)
    private BigDecimal openingQty = BigDecimal.ZERO;

    @Column(name = "opening_value", nullable = false)
    private BigDecimal openingValue = BigDecimal.ZERO;

    @Column(name = "closing_qty", nullable = false)
    private BigDecimal closingQty = BigDecimal.ZERO;

    @Column(name = "closing_value", nullable = false)
    private BigDecimal closingValue = BigDecimal.ZERO;
}
