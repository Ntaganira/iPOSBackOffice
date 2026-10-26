package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : DashboardService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Application service for dashboard.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.StockMovementRow;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.enums.ExpenseStatus;
import com.ntaganira.heritier.ipos.backoffice.repository.CustomerRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ExpenseRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.PurchaseRepository;
import com.ntaganira.heritier.ipos.backoffice.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private final ReportService reportService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRepository purchaseRepository;
    private final StockService stockService;

    public DashboardService(ReportService reportService,
                            ProductRepository productRepository,
                            CustomerRepository customerRepository,
                            ExpenseRepository expenseRepository,
                            PurchaseRepository purchaseRepository,
                            StockService stockService) {
        this.reportService = reportService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.expenseRepository = expenseRepository;
        this.purchaseRepository = purchaseRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public DashboardData load() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);
        ReportService.SalesReport todaySales = reportService.sales(today, today, null);
        ReportService.SalesReport week = reportService.sales(weekStart, today, null);
        ReportService.SalesReport month = reportService.sales(monthStart, today, null);

        List<DayPoint> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            BigDecimal net = week.days().stream()
                    .filter(day -> day.date().equals(date))
                    .map(ReportService.DayTotal::net)
                    .findFirst().orElse(BigDecimal.ZERO);
            trend.add(new DayPoint(DAY_LABEL.format(date), date, Money.of(net)));
        }

        BigDecimal customerDebt = customerRepository.findWithOutstandingBalance().stream()
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Product> lowStock = stockService.lowStock();
        List<StockMovementRow> recentMovements = stockService.recentMovements(8);
        List<Sale> recentSales = todaySales.sales().stream().limit(8).toList();

        return new DashboardData(
                todaySales,
                month,
                week,
                trend,
                productRepository.countByActiveTrue(),
                lowStock.size(),
                lowStock.stream().limit(6).toList(),
                Money.of(productRepository.sumStockValue()),
                Money.of(customerDebt),
                customerRepository.count(),
                expenseRepository.countByStatus(ExpenseStatus.PENDING),
                purchaseRepository.countByStatusIn(List.of(
                        com.ntaganira.heritier.ipos.backoffice.enums.PurchaseStatus.ORDERED,
                        com.ntaganira.heritier.ipos.backoffice.enums.PurchaseStatus.PARTIAL)),
                recentSales,
                recentMovements,
                week.topProducts().stream().limit(6).toList());
    }

    public record DayPoint(String label, LocalDate date, BigDecimal value) {
    }

    public record DashboardData(
            ReportService.SalesReport today,
            ReportService.SalesReport month,
            ReportService.SalesReport week,
            List<DayPoint> trend,
            long productCount,
            long lowStockCount,
            List<Product> lowStock,
            BigDecimal stockValue,
            BigDecimal customerDebt,
            long customerCount,
            long pendingExpenses,
            long pendingPurchases,
            List<Sale> recentSales,
            List<StockMovementRow> recentMovements,
            List<ReportService.ProductTotal> topProducts) {
    }
}
