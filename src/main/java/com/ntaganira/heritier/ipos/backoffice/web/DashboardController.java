package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.service.DashboardService;
import com.ntaganira.heritier.ipos.backoffice.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardService.DashboardData dashboard = dashboardService.load();
        List<DashboardService.DayPoint> trend = dashboard.trend();

        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendValues = new ArrayList<>();
        for (DashboardService.DayPoint point : trend) {
            trendLabels.add(point.label());
            trendValues.add(point.value());
        }

        List<String> paymentLabels = new ArrayList<>();
        List<BigDecimal> paymentValues = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : dashboard.week().paymentTotals().entrySet()) {
            if (entry.getValue() != null && entry.getValue().signum() > 0) {
                paymentLabels.add(entry.getKey());
                paymentValues.add(entry.getValue());
            }
        }

        List<String> topLabels = new ArrayList<>();
        List<BigDecimal> topValues = new ArrayList<>();
        for (ReportService.ProductTotal product : dashboard.topProducts()) {
            topLabels.add(product.name());
            topValues.add(product.revenue());
        }

        List<String> cashierLabels = new ArrayList<>();
        List<BigDecimal> cashierValues = new ArrayList<>();
        for (ReportService.CashierTotal cashier : dashboard.week().cashiers()) {
            cashierLabels.add(cashier.name());
            cashierValues.add(cashier.sales());
        }

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendValues", trendValues);
        model.addAttribute("paymentLabels", paymentLabels);
        model.addAttribute("paymentValues", paymentValues);
        model.addAttribute("topLabels", topLabels);
        model.addAttribute("topValues", topValues);
        model.addAttribute("cashierLabels", cashierLabels);
        model.addAttribute("cashierValues", cashierValues);
        model.addAttribute("trendChange", change(trendValues));
        return "dashboard";
    }

    private double change(List<BigDecimal> values) {
        if (values.size() < 2) {
            return 0;
        }
        BigDecimal previous = values.get(values.size() - 2);
        BigDecimal current = values.get(values.size() - 1);
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? 100 : 0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
