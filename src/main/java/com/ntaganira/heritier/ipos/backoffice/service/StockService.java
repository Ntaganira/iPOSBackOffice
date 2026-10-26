package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : StockService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for stock.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.StockMovementRow;
import com.ntaganira.heritier.ipos.backoffice.entity.InventoryMovement;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriod;
import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriodLine;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.enums.StockPeriodStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.InventoryMovementRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.StockPeriodLineRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.StockPeriodRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final StockPeriodRepository periodRepository;
    private final StockPeriodLineRepository periodLineRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public StockService(ProductRepository productRepository,
                        InventoryMovementRepository movementRepository,
                        StockPeriodRepository periodRepository,
                        StockPeriodLineRepository periodLineRepository,
                        SettingsService settingsService,
                        AuditService auditService,
                        CurrentUserService currentUser) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.periodRepository = periodRepository;
        this.periodLineRepository = periodLineRepository;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public InventoryMovement applyMovement(Product product, MovementType type, BigDecimal quantityDelta,
                                           BigDecimal unitCost, String referenceType, String referenceId,
                                           String note) {
        BigDecimal newStock = product.getStockQuantity().add(quantityDelta);
        ShopSetting settings = settingsService.get();
        if (newStock.signum() < 0 && !settings.isAllowNegativeStock()) {
            throw new BusinessException("Not enough stock for " + product.getName()
                    + ". Available: " + product.getStockQuantity().stripTrailingZeros().toPlainString() + ".");
        }
        product.setStockQuantity(newStock);
        productRepository.save(product);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantityDelta(quantityDelta);
        movement.setUnitCost(Money.of(unitCost));
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movement.setOccurredAt(Instant.now());
        movement.setCreatedBy(currentUser.username());
        return movementRepository.save(movement);
    }

    @Transactional
    public Product adjust(Long productId, BigDecimal quantityDelta, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        if (quantityDelta == null || quantityDelta.signum() == 0) {
            throw new BusinessException("Enter a quantity to add or remove.");
        }
        Map<String, Object> before = Map.of("stockQuantity", product.getStockQuantity());
        applyMovement(product, MovementType.ADJUSTMENT, quantityDelta, product.getBuyingPrice(),
                "ADJUSTMENT", null, note);
        auditService.updated("Product", product.getId(), product.getName(), before,
                Map.of("stockQuantity", product.getStockQuantity(), "adjustment", quantityDelta, "note", note));
        return product;
    }

    @Transactional
    public void receive(Long productId, BigDecimal quantity, BigDecimal unitCost, String note) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("Reception quantity must be greater than zero.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        Map<String, Object> before = Map.of("stockQuantity", product.getStockQuantity());
        applyMovement(product, MovementType.RECEPTION, quantity, unitCost,
                "RECEPTION", null, note);
        auditService.updated("Product", product.getId(), product.getName(), before,
                Map.of("stockQuantity", product.getStockQuantity(), "received", quantity, "note", note));
    }

    @Transactional(readOnly = true)
    public Page<StockMovementRow> movements(Long productId, MovementType type, String direction,
                                            Instant from, Instant to, String q, Pageable pageable) {
        return movementRepository.search(productId, type, direction, from, to, q, pageable)
                .map(m -> new StockMovementRow(
                        m.getId(),
                        m.getOccurredAt(),
                        m.getProduct().getName(),
                        m.getProduct().getCode(),
                        m.getMovementType(),
                        m.getQuantityDelta(),
                        m.getUnitCost(),
                        m.getReferenceType(),
                        m.getReferenceId(),
                        m.getNote(),
                        m.getCreatedBy()));
    }

    @Transactional
    public StockPeriod openPeriod(LocalDate start, LocalDate end, String notes) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new BusinessException("Enter a valid period start and end date.");
        }
        if (periodRepository.existsByStatus(StockPeriodStatus.OPEN)) {
            throw new BusinessException("Close the current open stock period before opening a new one.");
        }
        StockPeriod period = new StockPeriod();
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setStatus(StockPeriodStatus.OPEN);
        period.setNotes(notes);
        period.setOpenedBy(currentUser.username());
        period.setOpenedAt(Instant.now());
        for (Product product : productRepository.findByActiveTrueOrderByNameAsc()) {
            StockPeriodLine line = new StockPeriodLine();
            line.setProduct(product);
            line.setOpeningQty(product.getStockQuantity());
            line.setOpeningValue(Money.of(product.getStockQuantity().multiply(product.getBuyingPrice())));
            line.setClosingQty(product.getStockQuantity());
            line.setClosingValue(line.getOpeningValue());
            period.addLine(line);
        }
        StockPeriod saved = periodRepository.save(period);
        auditService.created("StockPeriod", saved.getId(),
                "Stock period " + start + " to " + end,
                Map.of("periodStart", start, "periodEnd", end, "status", "OPEN", "products", saved.getLines().size()));
        return saved;
    }

    @Transactional
    public StockPeriod closePeriod(Long id) {
        StockPeriod period = periodRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Stock period not found."));
        if (!period.isOpen()) {
            throw new BusinessException("This stock period is already closed.");
        }
        List<StockPeriodLine> lines = periodLineRepository.findByPeriodIdOrderByIdAsc(period.getId());
        for (StockPeriodLine line : lines) {
            Product product = line.getProduct();
            line.setClosingQty(product.getStockQuantity());
            line.setClosingValue(Money.of(product.getStockQuantity().multiply(product.getBuyingPrice())));
        }
        periodLineRepository.saveAll(lines);
        period.setStatus(StockPeriodStatus.CLOSED);
        period.setClosedBy(currentUser.username());
        period.setClosedAt(Instant.now());
        StockPeriod saved = periodRepository.save(period);
        auditService.record("UPDATE", "StockPeriod", saved.getId(),
                "Stock period " + period.getPeriodStart() + " to " + period.getPeriodEnd(),
                Map.of("status", "OPEN"), Map.of("status", "CLOSED"));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StockPeriod> periods() {
        return periodRepository.findAllByOrderByPeriodStartDesc();
    }

    @Transactional(readOnly = true)
    public StockPeriod period(Long id) {
        return periodRepository.findById(id).orElseThrow(() -> new BusinessException("Stock period not found."));
    }

    @Transactional(readOnly = true)
    public List<StockPeriodLine> periodLines(Long periodId) {
        return periodLineRepository.findByPeriodIdOrderByIdAsc(periodId);
    }

    @Transactional(readOnly = true)
    public List<Product> lowStock() {
        return productRepository.findLowStock();
    }

    @Transactional(readOnly = true)
    public List<StockMovementRow> recentMovements(int limit) {
        List<InventoryMovement> movements = movementRepository.findTop10ByOrderByOccurredAtDesc();
        List<StockMovementRow> rows = new ArrayList<>();
        for (InventoryMovement movement : movements) {
            rows.add(new StockMovementRow(movement.getId(), movement.getOccurredAt(),
                    movement.getProduct().getName(), movement.getProduct().getCode(),
                    movement.getMovementType(), movement.getQuantityDelta(), movement.getUnitCost(),
                    movement.getReferenceType(), movement.getReferenceId(), movement.getNote(),
                    movement.getCreatedBy()));
        }
        return rows.stream().limit(limit).toList();
    }
}
