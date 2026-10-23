package com.ntaganira.heritier.ipos.backoffice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tax_categories")
public class TaxCategory {

    @Id
    @Column(length = 2)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean exempt = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String label() {
        return code + " - " + name;
    }
}
