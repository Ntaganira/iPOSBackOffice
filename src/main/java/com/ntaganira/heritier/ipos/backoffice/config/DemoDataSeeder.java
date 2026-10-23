package com.ntaganira.heritier.ipos.backoffice.config;

import com.ntaganira.heritier.ipos.backoffice.entity.Category;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import com.ntaganira.heritier.ipos.backoffice.entity.TaxCategory;
import com.ntaganira.heritier.ipos.backoffice.entity.Unit;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.repository.CategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SupplierRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TaxCategoryRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.UnitRepository;
import com.ntaganira.heritier.ipos.backoffice.service.StockService;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties properties;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final TaxCategoryRepository taxCategoryRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;

    public DemoDataSeeder(AppProperties properties,
                          ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          UnitRepository unitRepository,
                          TaxCategoryRepository taxCategoryRepository,
                          CustomerRepository customerRepository,
                          SupplierRepository supplierRepository,
                          StockService stockService) {
        this.properties = properties;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.taxCategoryRepository = taxCategoryRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.demo().enabled()) {
            return;
        }
        if (productRepository.count() > 0) {
            return;
        }
        Map<String, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, Function.identity(), (a, b) -> a));
        Map<String, Unit> units = unitRepository.findAll().stream()
                .collect(Collectors.toMap(Unit::getName, Function.identity(), (a, b) -> a));
        Map<String, TaxCategory> taxes = taxCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(TaxCategory::getCode, Function.identity(), (a, b) -> a));
        if (!categories.containsKey("Beverages") || !units.containsKey("Piece")) {
            log.warn("Demo data was not seeded because reference data is missing.");
            return;
        }

        List<DemoProduct> products = List.of(
                new DemoProduct("BEV-001", "Inyange Water 500ml", "Beverages", "Piece", "A", 300, 500, 24, 120),
                new DemoProduct("BEV-002", "Coca-Cola 500ml", "Beverages", "Piece", "B", 550, 900, 24, 96),
                new DemoProduct("BEV-003", "Fanta Orange 500ml", "Beverages", "Piece", "B", 550, 900, 24, 96),
                new DemoProduct("BEV-004", "Milk 1L", "Beverages", "Litre", "A", 800, 1200, 12, 48),
                new DemoProduct("BEV-005", "Ground Coffee 250g", "Beverages", "Pack", "B", 2500, 3500, 6, 24),
                new DemoProduct("BEV-006", "Tea Leaves 250g", "Beverages", "Pack", "B", 1200, 1800, 6, 30),
                new DemoProduct("BEV-007", "Mango Juice 1L", "Beverages", "Litre", "B", 1800, 2500, 8, 36),
                new DemoProduct("FOO-001", "Rice 5kg", "Food", "Pack", "A", 6000, 8000, 5, 40),
                new DemoProduct("FOO-002", "Sugar 1kg", "Food", "Kilogram", "B", 1200, 1600, 10, 60),
                new DemoProduct("FOO-003", "Cooking Oil 1L", "Food", "Litre", "B", 2200, 3000, 10, 48),
                new DemoProduct("FOO-004", "Maize Flour 1kg", "Food", "Kilogram", "A", 800, 1200, 10, 50),
                new DemoProduct("FOO-005", "Beans 1kg", "Food", "Kilogram", "A", 1000, 1500, 10, 45),
                new DemoProduct("FOO-006", "Eggs Tray (30)", "Food", "Box", "A", 3000, 4000, 5, 25),
                new DemoProduct("FOO-007", "Fresh Bread", "Food", "Piece", "A", 700, 1000, 10, 30),
                new DemoProduct("FOO-008", "Salt 1kg", "Food", "Kilogram", "A", 300, 500, 8, 40),
                new DemoProduct("HOU-001", "Bathing Soap 250g", "Household", "Piece", "B", 400, 700, 12, 60),
                new DemoProduct("HOU-002", "Washing Detergent 1kg", "Household", "Pack", "B", 1500, 2200, 8, 35),
                new DemoProduct("HOU-003", "Toilet Paper 4pack", "Household", "Pack", "B", 1000, 1500, 10, 40),
                new DemoProduct("ELE-001", "Phone Charger", "Electronics", "Piece", "B", 2500, 4000, 5, 18),
                new DemoProduct("ELE-002", "AA Batteries 4pack", "Electronics", "Pack", "B", 800, 1500, 6, 30),
                new DemoProduct("STA-001", "Notebook A5", "Stationery", "Piece", "B", 500, 1000, 10, 50),
                new DemoProduct("STA-002", "Ballpoint Pen", "Stationery", "Piece", "B", 100, 200, 20, 100)
        );

        for (DemoProduct row : products) {
            Category category = categories.get(row.category());
            Unit unit = units.getOrDefault(row.unit(), units.get("Piece"));
            TaxCategory tax = taxes.getOrDefault(row.tax(), taxes.get("A"));
            if (category == null || unit == null || tax == null) {
                continue;
            }
            Product product = new Product();
            product.setCode(row.code());
            product.setName(row.name());
            product.setCategory(category);
            product.setUnit(unit);
            product.setTaxCategory(tax);
            product.setBuyingPrice(Money.of(BigDecimal.valueOf(row.buying())));
            product.setSellingPrice(Money.of(BigDecimal.valueOf(row.selling())));
            product.setReorderLevel(Money.quantity(BigDecimal.valueOf(row.reorder())));
            product.setStockQuantity(BigDecimal.ZERO);
            product.setActive(true);
            product.setCreatedBy("demo");
            Product saved = productRepository.save(product);
            if (row.stock() > 0) {
                stockService.applyMovement(saved, MovementType.OPENING,
                        Money.quantity(BigDecimal.valueOf(row.stock())), saved.getBuyingPrice(),
                        "DEMO", String.valueOf(saved.getId()), "Demo opening stock");
            }
        }

        if (customerRepository.count() == 0) {
            customerRepository.save(customer("Aline Uwase", "0788123456", "aline@example.rw",
                    CustomerType.INDIVIDUAL, "50000", null));
            customerRepository.save(customer("Kigali Retail Ltd", "0788987654", "accounts@kigaliretail.rw",
                    CustomerType.COMPANY, "300000", "102345678"));
            customerRepository.save(customer("Eric Niyonsenga", "0722334455", "eric@example.rw",
                    CustomerType.INDIVIDUAL, "20000", null));
        }
        if (supplierRepository.count() == 0) {
            supplierRepository.save(supplier("Kigali Wholesale Ltd", "0788111222",
                    "sales@kigaliwholesale.rw", "101234567"));
            supplierRepository.save(supplier("Rwanda Beverages Distributor", "0788333444",
                    "orders@rwandabeverages.rw", "102345678"));
        }
        log.info("Seeded demo catalogue ({} products), 3 customers and 2 suppliers.", products.size());
    }

    private Customer customer(String name, String phone, String email, CustomerType type,
                              String limit, String tin) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setCustomerType(type);
        customer.setCreditLimit(Money.of(new BigDecimal(limit)));
        customer.setTin(tin);
        customer.setActive(true);
        customer.setCreatedBy("demo");
        return customer;
    }

    private Supplier supplier(String name, String phone, String email, String tin) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setTin(tin);
        supplier.setActive(true);
        supplier.setCreatedBy("demo");
        return supplier;
    }

    private record DemoProduct(String code, String name, String category, String unit, String tax,
                               long buying, long selling, long reorder, long stock) {
    }
}
