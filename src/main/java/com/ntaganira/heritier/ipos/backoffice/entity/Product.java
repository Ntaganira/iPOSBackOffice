package com.ntaganira.heritier.ipos.backoffice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shop_products")
public class Product extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(unique = true, length = 80)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "buying_price", nullable = false)
    private BigDecimal buyingPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_code")
    private TaxCategory taxCategory;

    @Column(name = "reorder_level", nullable = false)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Version
    @Column(nullable = false)
    private long version;

    public boolean isLowStock() {
        return active && stockQuantity.compareTo(reorderLevel) <= 0;
    }

    public boolean isOutOfStock() {
        return active && stockQuantity.compareTo(BigDecimal.ZERO) <= 0;
    }
}
