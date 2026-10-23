package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.ImportForm;
import com.ntaganira.heritier.ipos.backoffice.entity.ImportBatch;
import com.ntaganira.heritier.ipos.backoffice.entity.ImportItem;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import com.ntaganira.heritier.ipos.backoffice.enums.ImportStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.repository.ImportBatchRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SupplierRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportService {

    private final ImportBatchRepository importRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public ImportService(ImportBatchRepository importRepository, ProductRepository productRepository,
                         SupplierRepository supplierRepository, StockService stockService,
                         AuditService auditService) {
        this.importRepository = importRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ImportBatch> search(String q, ImportStatus status, LocalDate from, LocalDate to,
                                    Pageable pageable) {
        return importRepository.search(q == null ? "" : q.trim().toLowerCase(), status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public ImportBatch get(Long id) {
        ImportBatch batch = importRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Import not found."));
        batch.getItems().size();
        return batch;
    }

    @Transactional(readOnly = true)
    public List<Product> products() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Supplier> suppliers() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public ImportBatch create(ImportForm form) {
        ImportBatch batch = new ImportBatch();
        batch.setReferenceNo(nextReference());
        apply(form, batch);
        ImportBatch saved = importRepository.save(batch);
        auditService.created("Import", saved.getId(), saved.getReferenceNo(),
                Map.of("totalLanded", saved.getTotalLanded(), "currency", saved.getCurrency()));
        return saved;
    }

    @Transactional
    public ImportBatch update(Long id, ImportForm form) {
        ImportBatch batch = get(id);
        if (!batch.isDraft()) {
            throw new BusinessException("Only draft imports can be edited.");
        }
        Map<String, Object> before = auditService.snapshot("totalLanded", batch.getTotalLanded(),
                "status", batch.getStatus());
        batch.getItems().clear();
        apply(form, batch);
        ImportBatch saved = importRepository.save(batch);
        auditService.updated("Import", saved.getId(), saved.getReferenceNo(), before,
                Map.of("totalLanded", saved.getTotalLanded(), "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public ImportBatch receive(Long id) {
        ImportBatch batch = get(id);
        if (batch.getStatus() != ImportStatus.DRAFT) {
            throw new BusinessException("Only draft imports can be received.");
        }
        if (batch.getItems().isEmpty()) {
            throw new BusinessException("Add at least one item before receiving this import.");
        }
        for (ImportItem item : batch.getItems()) {
            Product product = item.getProduct();
            BigDecimal beforeStock = product.getStockQuantity();
            Map<String, Object> before = auditService.snapshot("stockQuantity", beforeStock,
                    "buyingPrice", product.getBuyingPrice());
            stockService.applyMovement(product, MovementType.IMPORT, item.getQuantity(),
                    item.getLandedUnitCost(), "IMPORT", batch.getReferenceNo(),
                    "Import " + batch.getReferenceNo());
            product.setBuyingPrice(Money.of(item.getLandedUnitCost()));
            productRepository.save(product);
            auditService.updated("Product", product.getId(), product.getName(), before,
                    Map.of("stockQuantity", product.getStockQuantity(), "buyingPrice",
                            product.getBuyingPrice(), "import", batch.getReferenceNo()));
        }
        batch.setStatus(ImportStatus.RECEIVED);
        batch.setReceivedAt(Instant.now());
        ImportBatch saved = importRepository.save(batch);
        auditService.record("IMPORT_RECEIVED", "Import", saved.getId(), saved.getReferenceNo(),
                null, Map.of("totalLanded", saved.getTotalLanded()));
        return saved;
    }

    @Transactional
    public ImportBatch cancel(Long id) {
        ImportBatch batch = get(id);
        if (!batch.isDraft()) {
            throw new BusinessException("Only draft imports can be cancelled.");
        }
        batch.setStatus(ImportStatus.CANCELLED);
        ImportBatch saved = importRepository.save(batch);
        auditService.record("IMPORT_CANCELLED", "Import", saved.getId(), saved.getReferenceNo(),
                Map.of("status", "DRAFT"), Map.of("status", "CANCELLED"));
        return saved;
    }

    private void apply(ImportForm form, ImportBatch batch) {
        if (form.getImportDate() == null) {
            throw new BusinessException("Import date is required.");
        }
        if (form.getLines() == null || form.getLines().isEmpty()) {
            throw new BusinessException("Add at least one import line.");
        }
        BigDecimal rate = positive(form.getExchangeRate(), "Exchange rate");
        batch.setSupplier(resolveSupplier(form.getSupplierId()));
        batch.setOriginCountry(trimToNull(form.getOriginCountry()));
        batch.setCurrency(form.getCurrency() == null || form.getCurrency().isBlank()
                ? "USD" : form.getCurrency().trim().toUpperCase());
        batch.setExchangeRate(rate);
        batch.setCustomsDuty(nonNegative(form.getCustomsDuty(), "Customs duty"));
        batch.setTaxes(nonNegative(form.getTaxes(), "Taxes"));
        batch.setFreight(nonNegative(form.getFreight(), "Freight"));
        batch.setOtherCosts(nonNegative(form.getOtherCosts(), "Other costs"));
        batch.setImportDate(form.getImportDate());
        batch.setNotes(trimToNull(form.getNotes()));

        Map<Long, Product> products = new LinkedHashMap<>();
        BigDecimal foreignTotal = BigDecimal.ZERO;
        for (ImportForm.Line line : form.getLines()) {
            Product product = resolveProduct(line.getProductId());
            if (products.putIfAbsent(product.getId(), product) != null) {
                throw new BusinessException("Product " + product.getName() + " appears more than once.");
            }
            BigDecimal quantity = positive(line.getQuantity(), "Quantity for " + product.getName());
            BigDecimal unitCost = nonNegative(line.getUnitCostForeign(),
                    "Unit cost for " + product.getName());
            foreignTotal = foreignTotal.add(unitCost.multiply(quantity));
            ImportItem item = new ImportItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitCostForeign(unitCost);
            batch.addItem(item);
        }
        if (foreignTotal.signum() <= 0) {
            throw new BusinessException("The foreign goods value must be greater than zero.");
        }
        BigDecimal extras = batch.getCustomsDuty().add(batch.getTaxes())
                .add(batch.getFreight()).add(batch.getOtherCosts());
        BigDecimal totalLanded = BigDecimal.ZERO;
        for (ImportItem item : batch.getItems()) {
            BigDecimal foreignValue = item.foreignLineValue();
            BigDecimal share = foreignValue.multiply(extras)
                    .divide(foreignTotal, 6, RoundingMode.HALF_UP);
            BigDecimal landed = foreignValue.multiply(rate).add(share);
            BigDecimal unitLanded = landed.divide(item.getQuantity(), 2, RoundingMode.HALF_UP);
            item.setLandedUnitCost(unitLanded);
            item.setLineTotal(Money.of(unitLanded.multiply(item.getQuantity())));
            totalLanded = totalLanded.add(item.getLineTotal());
        }
        batch.setTotalLanded(Money.of(totalLanded));
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException("Supplier not found."));
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException("Choose a product for every line.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
    }

    private String nextReference() {
        String prefix = "IMP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        for (int attempt = 1; attempt <= 99; attempt++) {
            String candidate = prefix + String.format("%02d", attempt);
            if (!importRepository.existsByReferenceNoIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return prefix + System.currentTimeMillis() % 100000;
    }

    private BigDecimal positive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException(label + " must be greater than zero.");
        }
        return value;
    }

    private BigDecimal nonNegative(BigDecimal value, String label) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new BusinessException(label + " cannot be negative.");
        }
        return Money.of(value);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
