package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : PurchaseService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for purchase.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.PurchaseForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.entity.PurchaseLine;
import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.enums.PurchaseStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.PurchaseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SupplierRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;
    private final SequenceService sequenceService;
    private final AuditService auditService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           ProductRepository productRepository,
                           SupplierRepository supplierRepository,
                           StockService stockService,
                           SequenceService sequenceService,
                           AuditService auditService) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
        this.sequenceService = sequenceService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<Purchase> search(String q, Long supplierId, PurchaseStatus status,
                                 LocalDate from, LocalDate to, Pageable pageable) {
        return purchaseRepository.search(like(q), supplierId, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Purchase get(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Purchase not found."));
    }

    @Transactional
    public Purchase create(PurchaseForm form) {
        if (form.getLines() == null || form.getLines().isEmpty()) {
            throw new BusinessException("Add at least one product to the purchase.");
        }
        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber(sequenceService.next("PO-"));
        if (form.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(form.getSupplierId())
                    .orElseThrow(() -> new BusinessException("Supplier not found."));
            purchase.setSupplier(supplier);
        }
        purchase.setPurchaseDate(form.getPurchaseDate() == null ? LocalDate.now() : form.getPurchaseDate());
        purchase.setNotes(StringUtils.hasText(form.getNotes()) ? form.getNotes().trim() : null);
        purchase.setStatus(PurchaseStatus.ORDERED);

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseForm.Line row : form.getLines()) {
            if (row.getProductId() == null) {
                continue;
            }
            Product product = productRepository.findById(row.getProductId())
                    .orElseThrow(() -> new BusinessException("A product in this purchase no longer exists."));
            BigDecimal quantity = Money.quantity(row.getQuantity());
            if (quantity.signum() <= 0) {
                throw new BusinessException("Quantity for " + product.getName() + " must be greater than zero.");
            }
            BigDecimal unitCost = Money.of(row.getUnitCost());
            if (unitCost.signum() < 0) {
                throw new BusinessException("Unit cost cannot be negative.");
            }
            PurchaseLine line = new PurchaseLine();
            line.setProduct(product);
            line.setQuantityOrdered(quantity);
            line.setQuantityReceived(BigDecimal.ZERO);
            line.setUnitCost(unitCost);
            line.setLineTotal(Money.multiply(unitCost, quantity));
            purchase.addLine(line);
            total = total.add(line.getLineTotal());
        }
        if (purchase.getLines().isEmpty()) {
            throw new BusinessException("Add at least one valid product to the purchase.");
        }
        purchase.setSubtotal(Money.of(total));
        purchase.setTotal(Money.of(total));
        Purchase saved = purchaseRepository.save(purchase);
        auditService.created("Purchase", saved.getId(), saved.getPurchaseNumber(),
                Map.of("supplier", saved.getSupplier() == null ? "" : saved.getSupplier().getName(),
                        "total", saved.getTotal(), "status", saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public Purchase receive(Long id) {
        Purchase purchase = get(id);
        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BusinessException("This purchase has already been received.");
        }
        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new BusinessException("This purchase was cancelled.");
        }
        for (PurchaseLine line : purchase.getLines()) {
            BigDecimal remaining = line.getQuantityOrdered().subtract(line.getQuantityReceived());
            if (remaining.signum() > 0) {
                stockService.applyMovement(line.getProduct(), MovementType.PURCHASE, remaining,
                        line.getUnitCost(), "PURCHASE", purchase.getPurchaseNumber(),
                        "Received " + purchase.getPurchaseNumber());
                line.setQuantityReceived(line.getQuantityOrdered());
            }
        }
        purchase.setStatus(PurchaseStatus.RECEIVED);
        purchase.setReceivedAt(Instant.now());
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("UPDATE", "Purchase", saved.getId(), saved.getPurchaseNumber(),
                Map.of("status", "ORDERED"), Map.of("status", "RECEIVED"));
        return saved;
    }

    @Transactional
    public Purchase cancel(Long id) {
        Purchase purchase = get(id);
        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BusinessException("A received purchase cannot be cancelled.");
        }
        purchase.setStatus(PurchaseStatus.CANCELLED);
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("UPDATE", "Purchase", saved.getId(), saved.getPurchaseNumber(),
                Map.of("status", purchase.getStatus().name()), Map.of("status", "CANCELLED"));
        return saved;
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return purchaseRepository.countByStatusIn(List.of(PurchaseStatus.ORDERED, PurchaseStatus.PARTIAL));
    }

    @Transactional(readOnly = true)
    public List<Purchase> recent() {
        return purchaseRepository.findTop8ByOrderByPurchaseDateDesc();
    }

    public BigDecimal total(List<Purchase> purchases) {
        return purchases.stream().map(Purchase::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }
}
