package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : ReportController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for report.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.service.ReportService;
import com.ntaganira.heritier.ipos.backoffice.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        model.addAttribute("sales", reportService.sales(monthStart, today, null));
        model.addAttribute("profit", reportService.profit(monthStart, today));
        model.addAttribute("stock", reportService.stock(monthStart, today));
        model.addAttribute("from", monthStart);
        model.addAttribute("to", today);
        return "reports/index";
    }

    @GetMapping("/reports/sales")
    public String sales(@RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) Long cashierId,
                        Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        model.addAttribute("report", reportService.sales(start, end, cashierId));
        model.addAttribute("cashiers", userService.activeUsers());
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("cashierId", cashierId);
        return "reports/sales";
    }

    @GetMapping("/reports/stock")
    public String stock(@RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        model.addAttribute("report", reportService.stock(start, end));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "reports/stock";
    }

    @GetMapping("/reports/profit")
    public String profit(@RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to,
                         Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        model.addAttribute("report", reportService.profit(start, end));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "reports/profit";
    }

    @GetMapping("/reports/purchases")
    public String purchases(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        List<Purchase> purchases = reportService.purchases(start, end);
        model.addAttribute("purchases", purchases);
        model.addAttribute("total", purchases.stream().map(Purchase::getTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "reports/purchases";
    }

    @GetMapping("/reports/expenses")
    public String expenses(@RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        List<Expense> expenses = reportService.expenses(start, end);
        model.addAttribute("expenses", expenses);
        model.addAttribute("total", expenses.stream().map(Expense::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "reports/expenses";
    }

    @GetMapping("/reports/cash")
    public String cash(@RequestParam(required = false) LocalDate date,
                       @RequestParam(required = false) BigDecimal openingCash,
                       Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        BigDecimal opening = openingCash == null ? BigDecimal.ZERO : openingCash;
        model.addAttribute("summary", reportService.cashSummary(day, opening));
        model.addAttribute("date", day);
        model.addAttribute("openingCash", opening.max(BigDecimal.ZERO));
        return "reports/cash";
    }

    @GetMapping("/reports/sales.csv")
    public ResponseEntity<byte[]> salesCsv(@RequestParam(required = false) LocalDate from,
                                           @RequestParam(required = false) LocalDate to,
                                           @RequestParam(required = false) Long cashierId) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("sales-report-" + start + "-" + end + ".csv",
                reportService.salesCsv(reportService.sales(start, end, cashierId)));
    }

    @GetMapping("/reports/tax.csv")
    public ResponseEntity<byte[]> taxCsv(@RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("tax-report-" + start + "-" + end + ".csv",
                reportService.taxCsv(reportService.sales(start, end, null)));
    }

    @GetMapping("/reports/stock.csv")
    public ResponseEntity<byte[]> stockCsv(@RequestParam(required = false) LocalDate from,
                                           @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("stock-report-" + start + "-" + end + ".csv",
                reportService.stockCsv(reportService.stock(start, end)));
    }

    @GetMapping("/reports/movements.csv")
    public ResponseEntity<byte[]> movementsCsv(@RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("stock-movements-" + start + "-" + end + ".csv",
                reportService.movementsCsv(reportService.stock(start, end)));
    }

    @GetMapping("/reports/purchases.csv")
    public ResponseEntity<byte[]> purchasesCsv(@RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("purchases-" + start + "-" + end + ".csv",
                reportService.purchasesCsv(reportService.purchases(start, end)));
    }

    @GetMapping("/reports/expenses.csv")
    public ResponseEntity<byte[]> expensesCsv(@RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        return csv("expenses-" + start + "-" + end + ".csv",
                reportService.expensesCsv(reportService.expenses(start, end)));
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }
}
