package com.ntaganira.heritier.ipos.backoffice.entity;

import com.ntaganira.heritier.ipos.backoffice.enums.PaymentStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleType;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales")
public class Sale extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 40)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 10)
    private SaleType saleType = SaleType.SALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.COMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_of_sale_id")
    private Sale refundOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private AppUser cashier;

    @Column(name = "sale_date", nullable = false)
    private Instant saleDate = Instant.now();

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "change_due", nullable = false)
    private BigDecimal changeDue = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Column(length = 500)
    private String notes;

    @Column(name = "ebm_signature", length = 128)
    private String ebmSignature;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePayment> payments = new ArrayList<>();

    public void addLine(SaleLine line) {
        line.setSale(this);
        lines.add(line);
    }

    public void addPayment(SalePayment payment) {
        payment.setSale(this);
        payments.add(payment);
    }

    public boolean isRefund() {
        return saleType == SaleType.REFUND;
    }
}
