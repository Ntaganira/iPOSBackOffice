package com.ntaganira.heritier.ipos.backoffice.entity;

import com.ntaganira.heritier.ipos.backoffice.enums.ImportStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "imports")
public class ImportBatch extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 60)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "origin_country", length = 80)
    private String originCountry;

    @Column(nullable = false, length = 8)
    private String currency = "USD";

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "customs_duty", nullable = false)
    private BigDecimal customsDuty = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal taxes = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal freight = BigDecimal.ZERO;

    @Column(name = "other_costs", nullable = false)
    private BigDecimal otherCosts = BigDecimal.ZERO;

    @Column(name = "total_landed", nullable = false)
    private BigDecimal totalLanded = BigDecimal.ZERO;

    @Column(name = "import_date", nullable = false)
    private LocalDate importDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status = ImportStatus.DRAFT;

    @Column(length = 500)
    private String notes;

    @Column(name = "received_at")
    private Instant receivedAt;

    @OneToMany(mappedBy = "importBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportItem> items = new ArrayList<>();

    public boolean isDraft() {
        return status == ImportStatus.DRAFT;
    }

    public void addItem(ImportItem item) {
        item.setImportBatch(this);
        items.add(item);
    }
}
