package com.ntaganira.heritier.ipos.backoffice.service;

import com.ntaganira.heritier.ipos.backoffice.dto.ProductForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Category;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.TaxCategory;
import com.ntaganira.heritier.ipos.backoffice.entity.Unit;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.repository.CategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TaxCategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.UnitRepository;
import com.ntaganira.heritier.ipos.backoffice.util.CsvUtils;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final TaxCategoryRepository taxCategoryRepository;
    private final StockService stockService;
    private final SequenceService sequenceService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          UnitRepository unitRepository,
                          TaxCategoryRepository taxCategoryRepository,
                          StockService stockService,
                          SequenceService sequenceService,
                          StorageService storageService,
                          AuditService auditService,
                          CurrentUserService currentUser) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.taxCategoryRepository = taxCategoryRepository;
        this.stockService = stockService;
        this.sequenceService = sequenceService;
        this.storageService = storageService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String q, Long categoryId, Boolean active, Pageable pageable) {
        return productRepository.search(like(q), categoryId, active, pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> forPos(String q, int limit) {
        return productRepository.searchForPos(like(q), PageRequest.of(0, Math.max(1, Math.min(limit, 50))));
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found."));
    }

    @Transactional(readOnly = true)
    public ProductForm formFor(Long id) {
        return ProductForm.from(get(id));
    }

    @Transactional
    public Product create(ProductForm form, MultipartFile image) {
        String code = form.getCode().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("A product with code " + code + " already exists.");
        }
        if (StringUtils.hasText(form.getBarcode()) && productRepository.existsByBarcode(form.getBarcode().trim())) {
            throw new BusinessException("A product with this barcode already exists.");
        }
        Product product = new Product();
        product.setCode(code);
        applyForm(product, form);
        product.setStockQuantity(BigDecimal.ZERO);
        product.setCreatedBy(currentUser.username());
        if (image != null && !image.isEmpty()) {
            product.setImagePath(storageService.saveProductImage(image));
        } else if (StringUtils.hasText(form.getImagePath())) {
            product.setImagePath(form.getImagePath());
        }
        Product saved = productRepository.save(product);
        if (form.getOpeningQuantity() != null && form.getOpeningQuantity().signum() > 0) {
            stockService.applyMovement(saved, MovementType.OPENING, form.getOpeningQuantity(),
                    saved.getBuyingPrice(), "OPENING", String.valueOf(saved.getId()), "Opening stock");
        }
        auditService.created("Product", saved.getId(), saved.getCode() + " - " + saved.getName(), snapshot(saved));
        return saved;
    }

    @Transactional
    public Product update(Long id, ProductForm form, MultipartFile image) {
        Product product = get(id);
        Map<String, Object> before = snapshot(product);
        String code = form.getCode().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("A product with code " + code + " already exists.");
        }
        String barcode = StringUtils.hasText(form.getBarcode()) ? form.getBarcode().trim() : null;
        if (barcode != null && productRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw new BusinessException("A product with this barcode already exists.");
        }
        product.setCode(code);
        applyForm(product, form);
        String oldImage = product.getImagePath();
        if (image != null && !image.isEmpty()) {
            product.setImagePath(storageService.saveProductImage(image));
            if (oldImage != null) {
                storageService.delete(oldImage);
            }
        }
        Product saved = productRepository.save(product);
        auditService.updated("Product", saved.getId(), saved.getCode() + " - " + saved.getName(),
                before, snapshot(saved));
        return saved;
    }

    @Transactional
    public void toggleActive(Long id) {
        Product product = get(id);
        Map<String, Object> before = Map.of("active", product.isActive());
        product.setActive(!product.isActive());
        productRepository.save(product);
        auditService.updated("Product", product.getId(), product.getCode() + " - " + product.getName(),
                before, Map.of("active", product.isActive()));
    }

    @Transactional(readOnly = true)
    public List<Category> categories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Unit> units() {
        return unitRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Unit> activeUnits() {
        return unitRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<TaxCategory> taxCategories() {
        return taxCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public Category saveCategory(Long id, String name, boolean active) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("Category name is required.");
        }
        Category category = id == null ? new Category() : categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found."));
        String clean = name.trim();
        if (category.getId() == null && categoryRepository.existsByNameIgnoreCase(clean)) {
            throw new BusinessException("A category with this name already exists.");
        }
        Map<String, Object> before = category.getId() == null ? null
                : Map.of("name", category.getName(), "active", category.isActive());
        category.setName(clean);
        category.setActive(active);
        Category saved = categoryRepository.save(category);
        if (before == null) {
            auditService.created("Category", saved.getId(), saved.getName(),
                    Map.of("name", saved.getName(), "active", saved.isActive()));
        } else {
            auditService.updated("Category", saved.getId(), saved.getName(), before,
                    Map.of("name", saved.getName(), "active", saved.isActive()));
        }
        return saved;
    }

    @Transactional
    public Unit saveUnit(Long id, String name, String code, boolean active) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("Unit name is required.");
        }
        Unit unit = id == null ? new Unit() : unitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Unit not found."));
        String clean = name.trim();
        if (unit.getId() == null && unitRepository.existsByNameIgnoreCase(clean)) {
            throw new BusinessException("A unit with this name already exists.");
        }
        Map<String, Object> before = unit.getId() == null ? null
                : Map.of("name", unit.getName(), "code", unit.getCode() == null ? "" : unit.getCode(),
                "active", unit.isActive());
        unit.setName(clean);
        unit.setCode(StringUtils.hasText(code) ? code.trim() : clean);
        unit.setActive(active);
        Unit saved = unitRepository.save(unit);
        Map<String, Object> after = Map.of("name", saved.getName(),
                "code", saved.getCode() == null ? "" : saved.getCode(), "active", saved.isActive());
        if (before == null) {
            auditService.created("Unit", saved.getId(), saved.getName(), after);
        } else {
            auditService.updated("Unit", saved.getId(), saved.getName(), before, after);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write('\uFEFF');
            out.write(("code,barcode,name,category,unit,buying_price,selling_price,tax_category,"
                    + "reorder_level,stock,active\r\n").getBytes(StandardCharsets.UTF_8));
            for (Product product : productRepository.findAll()) {
                out.write(CsvUtils.line(
                        product.getCode(),
                        product.getBarcode(),
                        product.getName(),
                        product.getCategory() == null ? "" : product.getCategory().getName(),
                        product.getUnit() == null ? "" : product.getUnit().getName(),
                        product.getBuyingPrice(),
                        product.getSellingPrice(),
                        product.getTaxCategory() == null ? "" : product.getTaxCategory().getCode(),
                        product.getReorderLevel(),
                        product.getStockQuantity(),
                        product.isActive()).getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Could not build the product export.", ex);
        }
    }

    @Transactional
    public ImportResult importCsv(InputStream input) {
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        Map<String, Category> categoryCache = new HashMap<>();
        Map<String, Unit> unitCache = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            categoryCache.put(category.getName().toLowerCase(Locale.ROOT), category);
        }
        for (Unit unit : unitRepository.findAll()) {
            unitCache.put(unit.getName().toLowerCase(Locale.ROOT), unit);
        }
        Map<String, Integer> columns = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException("The CSV file is empty.");
            }
            header = header.replace("\uFEFF", "");
            List<String> headers = CsvUtils.parse(header);
            for (int i = 0; i < headers.size(); i++) {
                columns.put(headers.get(i).toLowerCase(Locale.ROOT).replace(" ", "_"), i);
            }
            if (!columns.containsKey("name") || !columns.containsKey("code")) {
                throw new BusinessException("The CSV must include at least the code and name columns.");
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    List<String> values = CsvUtils.parse(line);
                    String code = value(values, columns, "code").toUpperCase(Locale.ROOT);
                    String name = value(values, columns, "name");
                    if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
                        errors.add("Row " + row + ": code and name are required.");
                        continue;
                    }
                    String categoryName = value(values, columns, "category");
                    String unitName = value(values, columns, "unit");
                    Category category = categoryCache.get(categoryName.toLowerCase(Locale.ROOT));
                    Unit unit = unitCache.get(unitName.toLowerCase(Locale.ROOT));
                    if (category == null) {
                        errors.add("Row " + row + ": unknown category '" + categoryName + "'.");
                        continue;
                    }
                    if (unit == null) {
                        errors.add("Row " + row + ": unknown unit '" + unitName + "'.");
                        continue;
                    }
                    Product product = productRepository.findByCodeIgnoreCase(code).orElse(null);
                    boolean isNew = product == null;
                    if (isNew) {
                        product = new Product();
                        product.setCode(code);
                        product.setStockQuantity(BigDecimal.ZERO);
                        product.setCreatedBy(currentUser.username());
                    }
                    product.setName(name);
                    String barcode = value(values, columns, "barcode");
                    product.setBarcode(StringUtils.hasText(barcode) ? barcode : null);
                    product.setCategory(category);
                    product.setUnit(unit);
                    String taxCode = value(values, columns, "tax_category");
                    TaxCategory tax = taxCategoryRepository.findById(StringUtils.hasText(taxCode) ? taxCode : "A")
                            .orElse(null);
                    product.setTaxCategory(tax);
                    product.setBuyingPrice(parseDecimal(value(values, columns, "buying_price"),
                            product.getBuyingPrice() == null ? BigDecimal.ZERO : product.getBuyingPrice()));
                    product.setSellingPrice(parseDecimal(value(values, columns, "selling_price"),
                            product.getSellingPrice() == null ? BigDecimal.ZERO : product.getSellingPrice()));
                    product.setReorderLevel(parseDecimal(value(values, columns, "reorder_level"),
                            product.getReorderLevel() == null ? BigDecimal.ZERO : product.getReorderLevel()));
                    String active = value(values, columns, "active");
                    product.setActive(!StringUtils.hasText(active) || !active.equalsIgnoreCase("false"));
                    Product saved = productRepository.save(product);
                    if (isNew) {
                        BigDecimal stock = parseDecimal(value(values, columns, "stock"), BigDecimal.ZERO);
                        if (stock.signum() > 0) {
                            stockService.applyMovement(saved, MovementType.OPENING, stock,
                                    saved.getBuyingPrice(), "IMPORT", String.valueOf(saved.getId()), "CSV import");
                        }
                        created++;
                    } else {
                        updated++;
                    }
                } catch (RuntimeException ex) {
                    errors.add("Row " + row + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("Could not read the CSV file.", ex);
        }
        auditService.record("IMPORT", "Product", null, "Product CSV import",
                null, Map.of("created", created, "updated", updated, "errors", errors.size()));
        return new ImportResult(created, updated, errors);
    }

    private void applyForm(Product product, ProductForm form) {
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new BusinessException("Choose a valid category."));
        Unit unit = unitRepository.findById(form.getUnitId())
                .orElseThrow(() -> new BusinessException("Choose a valid unit."));
        TaxCategory tax = taxCategoryRepository.findById(form.getTaxCategoryCode())
                .orElseThrow(() -> new BusinessException("Choose a valid tax category."));
        product.setName(form.getName().trim());
        product.setBarcode(StringUtils.hasText(form.getBarcode()) ? form.getBarcode().trim() : null);
        product.setCategory(category);
        product.setUnit(unit);
        product.setTaxCategory(tax);
        product.setBuyingPrice(Money.of(form.getBuyingPrice()));
        product.setSellingPrice(Money.of(form.getSellingPrice()));
        product.setReorderLevel(Money.quantity(form.getReorderLevel()));
        product.setActive(form.isActive());
    }

    private Map<String, Object> snapshot(Product product) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("code", product.getCode());
        map.put("barcode", product.getBarcode());
        map.put("name", product.getName());
        map.put("category", product.getCategory() == null ? null : product.getCategory().getName());
        map.put("unit", product.getUnit() == null ? null : product.getUnit().getName());
        map.put("taxCategory", product.getTaxCategory() == null ? null : product.getTaxCategory().getCode());
        map.put("buyingPrice", product.getBuyingPrice());
        map.put("sellingPrice", product.getSellingPrice());
        map.put("reorderLevel", product.getReorderLevel());
        map.put("stockQuantity", product.getStockQuantity());
        map.put("active", product.isActive());
        map.put("imagePath", product.getImagePath());
        return map;
    }

    private String value(List<String> values, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        if (index == null || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private BigDecimal parseDecimal(String value, BigDecimal fallback) {
        if (!StringUtils.hasText(value)) {
            return Money.of(fallback);
        }
        try {
            return Money.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException ex) {
            throw new BusinessException("'" + value + "' is not a valid number.");
        }
    }

    private String like(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
    }

    public record ImportResult(int created, int updated, List<String> errors) {
    }
}
