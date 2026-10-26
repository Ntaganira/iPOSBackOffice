package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : ReportService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Builds sales, stock, profit and cash reports.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.entity.InventoryMovement;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.entity.SalePayment;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleType;
import com.ntaganira.heritier.ipos.backoffice.repository.ExpenseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.InventoryMovementRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.PurchaseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SalePaymentRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.SaleRepository;
import com.ntaganira.heritier.ipos.backoffice.util.CsvUtils;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final SettingsService settingsService;

    public ReportService(SaleRepository saleRepository,
                         ProductRepository productRepository,
                         InventoryMovementRepository movementRepository,
                         PurchaseRepository purchaseRepository,
                         ExpenseRepository expenseRepository,
                         SalePaymentRepository salePaymentRepository,
                         SettingsService settingsService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public SalesReport sales(LocalDate from, LocalDate to, Long cashierId) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<Sale> sales = saleRepository.findBySaleDateBetweenOrderBySaleDateAsc(start, end).stream()
                .filter(sale -> cashierId == null
                        || (sale.getCashier() != null && cashierId.equals(sale.getCashier().getId())))
                .toList();

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;
        int saleCount = 0;
        int refundCount = 0;
        Map<String, BigDecimal> paymentTotals = new LinkedHashMap<>();
        Map<String, CashierTotal> cashiers = new TreeMap<>();
        Map<LocalDate, DayTotal> days = new TreeMap<>();
        Map<String, ProductTotal> products = new TreeMap<>();
        Map<String, TaxLine> taxes = new TreeMap<>();

        for (Sale sale : sales) {
            if (sale.getStatus() == com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus.VOID) {
                continue;
            }
            boolean refund = sale.getSaleType() == SaleType.REFUND;
            BigDecimal sign = refund ? BigDecimal.ONE.negate() : BigDecimal.ONE;
            if (refund) {
                refunds = refunds.add(sale.getTotal());
                refundCount++;
            } else {
                gross = gross.add(sale.getTotal());
                saleCount++;
            }
            tax = tax.add(sale.getTaxTotal().multiply(sign));
            discounts = discounts.add(sale.getDiscountTotal().multiply(sign));

            LocalDate day = sale.getSaleDate().atZone(zone).toLocalDate();
            DayTotal dayTotal = days.computeIfAbsent(day, DayTotal::empty);
            dayTotal = refund
                    ? new DayTotal(day, dayTotal.sales(), dayTotal.refunds().add(sale.getTotal()), dayTotal.count())
                    : new DayTotal(day, dayTotal.sales().add(sale.getTotal()), dayTotal.refunds(), dayTotal.count() + 1);
            days.put(day, dayTotal);

            String cashierName = sale.getCashier() == null ? "Unassigned" : sale.getCashier().getFullName();
            CashierTotal cashierTotal = cashiers.getOrDefault(cashierName, new CashierTotal(cashierName, 0, BigDecimal.ZERO));
            cashierTotal = new CashierTotal(cashierName,
                    cashierTotal.count() + (refund ? 0 : 1),
                    cashierTotal.sales().add(sale.getTotal().multiply(sign)));
            cashiers.put(cashierName, cashierTotal);

            if (!refund) {
                for (SalePayment payment : sale.getPayments()) {
                    paymentTotals.merge(payment.getPaymentMethod().name(), payment.getAmount(), BigDecimal::add);
                }
            }
            for (SaleLine line : sale.getLines()) {
                BigDecimal quantity = line.getQuantity().multiply(sign);
                BigDecimal revenue = line.getLineTotal().multiply(sign);
                BigDecimal cost = Money.multiply(line.getUnitCost(), line.getQuantity()).multiply(sign);
                cogs = cogs.add(cost);
                String key = line.getProductCode() == null ? line.getProductName() : line.getProductCode();
                ProductTotal total = products.getOrDefault(key,
                        new ProductTotal(key, line.getProductName(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
                products.put(key, new ProductTotal(key, line.getProductName(),
                        total.quantity().add(quantity),
                        total.revenue().add(revenue),
                        total.cost().add(cost)));

                String code = line.getTaxCategoryCode() == null ? "A" : line.getTaxCategoryCode();
                TaxLine taxLine = taxes.getOrDefault(code, new TaxLine(code, BigDecimal.ZERO, BigDecimal.ZERO));
                taxes.put(code, new TaxLine(code,
                        taxLine.taxable().add(line.getLineTotal().multiply(sign)),
                        taxLine.tax().add(line.getTaxAmount().multiply(sign))));
            }
        }

        BigDecimal netSales = gross.subtract(refunds);
        BigDecimal netTax = tax;
        BigDecimal revenueExTax = netSales.subtract(netTax);
        BigDecimal grossProfit = revenueExTax.subtract(cogs);
        int transactions = saleCount;
        BigDecimal average = transactions == 0 ? BigDecimal.ZERO
                : netSales.divide(BigDecimal.valueOf(transactions), 2, RoundingMode.HALF_UP);

        List<ProductTotal> topProducts = products.values().stream()
                .filter(product -> product.quantity().signum() != 0)
                .sorted(Comparator.comparing(ProductTotal::revenue).reversed())
                .limit(12)
                .toList();

        return new SalesReport(from, to,
                Money.of(gross), Money.of(refunds), Money.of(netSales), Money.of(netTax),
                Money.of(discounts), Money.of(cogs), Money.of(grossProfit),
                saleCount, refundCount, average,
                new ArrayList<>(days.values()),
                paymentTotals,
                new ArrayList<>(cashiers.values()),
                topProducts,
                new ArrayList<>(taxes.values()),
                sales.stream().sorted(Comparator.comparing(Sale::getSaleDate).reversed()).limit(100).toList());
    }

    @Transactional(readOnly = true)
    public CashSummary cashSummary(LocalDate date, BigDecimal openingCash) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal opening = openingCash == null ? BigDecimal.ZERO : openingCash.max(BigDecimal.ZERO);
        BigDecimal cashSales = Money.of(salePaymentRepository.sumByMethod(start, end, PaymentMethod.CASH));
        List<Sale> sales = saleRepository.findBySaleDateBetweenOrderBySaleDateAsc(start, end);
        int cashSaleCount = 0;
        BigDecimal cashRefunds = BigDecimal.ZERO;
        int refundCount = 0;
        for (Sale sale : sales) {
            if (sale.getSaleType() == SaleType.SALE && sale.getStatus() == SaleStatus.COMPLETED
                    && cashPaid(sale).signum() > 0) {
                cashSaleCount++;
            }
            if (sale.getSaleType() == SaleType.REFUND && sale.getStatus() == SaleStatus.COMPLETED) {
                refundCount++;
                if (sale.getRefundOf() != null) {
                    cashRefunds = cashRefunds.add(cashPaid(sale.getRefundOf()));
                }
            }
        }
        BigDecimal expenses = Money.of(expenseRepository.sumApprovedCash(date, date));
        BigDecimal expected = Money.of(opening.add(cashSales).subtract(cashRefunds).subtract(expenses));
        return new CashSummary(date, Money.of(opening), cashSales, Money.of(cashRefunds),
                expenses, expected, cashSaleCount, refundCount);
    }

    private BigDecimal cashPaid(Sale sale) {
        return salePaymentRepository.findBySaleIdOrderByIdAsc(sale.getId()).stream()
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.CASH)
                .map(SalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public ProfitReport profit(LocalDate from, LocalDate to) {
        SalesReport sales = sales(from, to, null);
        BigDecimal expenses = Money.of(expenseRepository.totalApproved(from, to));
        BigDecimal netProfit = sales.grossProfit().subtract(expenses);
        return new ProfitReport(sales, expenses, Money.of(netProfit));
    }

    @Transactional(readOnly = true)
    public StockReport stock(LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<InventoryMovement> movements = movementRepository
                .findByOccurredAtBetweenOrderByOccurredAtAsc(start, end);
        Map<MovementType, BigDecimal> inByType = new LinkedHashMap<>();
        Map<MovementType, BigDecimal> outByType = new LinkedHashMap<>();
        for (InventoryMovement movement : movements) {
            if (movement.getQuantityDelta().signum() >= 0) {
                inByType.merge(movement.getMovementType(), movement.getQuantityDelta(), BigDecimal::add);
            } else {
                outByType.merge(movement.getMovementType(), movement.getQuantityDelta().abs(), BigDecimal::add);
            }
        }
        List<Product> products = productRepository.findAll();
        List<Product> lowStock = products.stream()
                .filter(product -> product.isActive() && product.isLowStock())
                .sorted(Comparator.comparing(Product::getStockQuantity))
                .toList();
        BigDecimal stockValue = Money.of(productRepository.sumStockValue());
        BigDecimal stockQuantity = productRepository.sumStockQuantity();
        return new StockReport(from, to, movements, inByType, outByType,
                products, lowStock, stockValue, stockQuantity);
    }

    @Transactional(readOnly = true)
    public List<Purchase> purchases(LocalDate from, LocalDate to) {
        return purchaseRepository.findAll().stream()
                .filter(purchase -> !purchase.getPurchaseDate().isBefore(from)
                        && !purchase.getPurchaseDate().isAfter(to))
                .sorted(Comparator.comparing(Purchase::getPurchaseDate).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Expense> expenses(LocalDate from, LocalDate to) {
        return expenseRepository.search(from, to, null, null, null, null, "",
                org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    public byte[] salesCsv(SalesReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Receipt,Date,Type,Status,Customer,Cashier,Subtotal,Discount,Tax,Total,Paid,Balance,Payment status\r\n");
        for (Sale sale : report.sales()) {
            csv.append(CsvUtils.line(
                    sale.getReceiptNumber(),
                    sale.getSaleDate().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    sale.getSaleType(),
                    sale.getStatus(),
                    sale.getCustomer() == null ? "Walk-in" : sale.getCustomer().getName(),
                    sale.getCashier() == null ? "" : sale.getCashier().getFullName(),
                    sale.getSubtotal(),
                    sale.getDiscountTotal(),
                    sale.getTaxTotal(),
                    sale.getTotal(),
                    sale.getAmountPaid(),
                    sale.getBalanceDue(),
                    sale.getPaymentStatus()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] stockCsv(StockReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Code,Barcode,Name,Category,Unit,Stock,Reorder level,Buying price,Selling price,Stock value,Active\r\n");
        for (Product product : report.products()) {
            csv.append(CsvUtils.line(
                    product.getCode(),
                    product.getBarcode(),
                    product.getName(),
                    product.getCategory() == null ? "" : product.getCategory().getName(),
                    product.getUnit() == null ? "" : product.getUnit().getName(),
                    product.getStockQuantity(),
                    product.getReorderLevel(),
                    product.getBuyingPrice(),
                    product.getSellingPrice(),
                    Money.multiply(product.getStockQuantity(), product.getBuyingPrice()),
                    product.isActive()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] movementsCsv(StockReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Date,Product,Code,Type,Quantity,Unit cost,Reference,Note,User\r\n");
        for (InventoryMovement movement : report.movements()) {
            csv.append(CsvUtils.line(
                    movement.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    movement.getProduct().getName(),
                    movement.getProduct().getCode(),
                    movement.getMovementType(),
                    movement.getQuantityDelta(),
                    movement.getUnitCost(),
                    movement.getReferenceType() + (movement.getReferenceId() == null ? "" : " " + movement.getReferenceId()),
                    movement.getNote(),
                    movement.getCreatedBy()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] purchasesCsv(List<Purchase> purchases) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Number,Date,Supplier,Status,Payment status,Subtotal,Tax,Total,Paid,Balance\r\n");
        for (Purchase purchase : purchases) {
            csv.append(CsvUtils.line(
                    purchase.getPurchaseNumber(),
                    purchase.getPurchaseDate(),
                    purchase.getSupplier() == null ? "" : purchase.getSupplier().getName(),
                    purchase.getStatus(),
                    purchase.getPaymentStatus(),
                    purchase.getSubtotal(),
                    purchase.getTaxTotal(),
                    purchase.getTotal(),
                    purchase.getAmountPaid(),
                    purchase.getBalance()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] expensesCsv(List<Expense> expenses) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Date,Category,Description,Payee,Method,Reference,Amount,Tax,Status\r\n");
        for (Expense expense : expenses) {
            csv.append(CsvUtils.line(
                    expense.getExpenseDate(),
                    expense.getCategory().getName(),
                    expense.getDescription(),
                    expense.getPayee(),
                    expense.getPaymentMethod(),
                    expense.getReference(),
                    expense.getAmount(),
                    expense.getTaxAmount(),
                    expense.getStatus()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] taxCsv(SalesReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Tax category,Taxable amount,VAT\r\n");
        for (TaxLine line : report.taxSummary()) {
            csv.append(CsvUtils.line(line.code(), line.taxable(), line.tax()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public String currency() {
        return settingsService.get().getCurrency();
    }

    public record SalesReport(
            LocalDate from, LocalDate to,
            BigDecimal grossSales, BigDecimal refunds, BigDecimal netSales, BigDecimal tax,
            BigDecimal discounts, BigDecimal cogs, BigDecimal grossProfit,
            int transactions, int refundCount, BigDecimal averageSale,
            List<DayTotal> days, Map<String, BigDecimal> paymentTotals,
            List<CashierTotal> cashiers, List<ProductTotal> topProducts,
            List<TaxLine> taxSummary, List<Sale> sales) {
    }

    public record DayTotal(LocalDate date, BigDecimal sales, BigDecimal refunds, int count) {
        public static DayTotal empty(LocalDate date) {
            return new DayTotal(date, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        public BigDecimal net() {
            return sales.subtract(refunds);
        }
    }

    public record CashierTotal(String name, int count, BigDecimal sales) {
    }

    public record ProductTotal(String code, String name, BigDecimal quantity, BigDecimal revenue, BigDecimal cost) {
        public BigDecimal profit() {
            return revenue.subtract(cost);
        }
    }

    public record TaxLine(String code, BigDecimal taxable, BigDecimal tax) {
    }

    public record ProfitReport(SalesReport sales, BigDecimal expenses, BigDecimal netProfit) {
    }

    public record CashSummary(LocalDate date, BigDecimal openingCash, BigDecimal cashSales,
                              BigDecimal cashRefunds, BigDecimal cashExpenses, BigDecimal expectedCash,
                              int cashSaleCount, int refundCount) {
    }

    public record StockReport(
            LocalDate from, LocalDate to, List<InventoryMovement> movements,
            Map<MovementType, BigDecimal> inByType, Map<MovementType, BigDecimal> outByType,
            List<Product> products, List<Product> lowStock,
            BigDecimal stockValue, BigDecimal stockQuantity) {
    }
}
