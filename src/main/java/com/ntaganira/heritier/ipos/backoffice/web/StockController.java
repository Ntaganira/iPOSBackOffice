package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.StockMovementRow;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.entity.StockPeriod;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.service.ProductService;
import com.ntaganira.heritier.ipos.backoffice.service.ReportService;
import com.ntaganira.heritier.ipos.backoffice.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@PreAuthorize("hasAuthority('STOCK_VIEW')")
public class StockController {

    private final StockService stockService;
    private final ProductService productService;
    private final ReportService reportService;

    public StockController(StockService stockService, ProductService productService, ReportService reportService) {
        this.stockService = stockService;
        this.productService = productService;
        this.reportService = reportService;
    }

    @GetMapping("/stock")
    public String stock(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long locationId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "30") int size,
                        Model model) {
        Page<Product> products = productService.search(q, null, true, locationId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "name")));
        LocalDate today = LocalDate.now();
        ReportService.StockReport report = reportService.stock(today, today);
        model.addAttribute("products", products);
        model.addAttribute("locations", productService.locations());
        model.addAttribute("q", q);
        model.addAttribute("locationId", locationId);
        model.addAttribute("stockValue", report.stockValue());
        model.addAttribute("stockQuantity", report.stockQuantity());
        model.addAttribute("lowStock", stockService.lowStock());
        model.addAttribute("recentMovements", stockService.recentMovements(8));
        return "stock/index";
    }

    @GetMapping("/stock/movements")
    public String movements(@RequestParam(required = false) Long productId,
                            @RequestParam(required = false) MovementType type,
                            @RequestParam(required = false) String direction,
                            @RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "40") int size,
                            Model model) {
        Page<StockMovementRow> movements = stockService.movements(productId, type, direction,
                null, null, q, PageRequest.of(Math.max(0, page), Math.min(200, Math.max(5, size)),
                        Sort.by(Sort.Direction.DESC, "occurredAt")));
        model.addAttribute("movements", movements);
        model.addAttribute("products", productService.search("", null, true,
                PageRequest.of(0, 500, Sort.by("name"))).getContent());
        model.addAttribute("types", MovementType.values());
        model.addAttribute("productId", productId);
        model.addAttribute("type", type);
        model.addAttribute("direction", direction);
        model.addAttribute("q", q);
        return "stock/movements";
    }

    @PostMapping("/stock/adjust")
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public String adjust(@RequestParam Long productId,
                         @RequestParam BigDecimal quantityDelta,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirect) {
        try {
            stockService.adjust(productId, quantityDelta, note);
            redirect.addFlashAttribute("success", "Stock adjusted.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stock";
    }

    @PostMapping("/stock/receive")
    @PreAuthorize("hasAuthority('STOCK_RECEPTION')")
    public String receive(@RequestParam Long productId,
                          @RequestParam BigDecimal quantity,
                          @RequestParam(required = false) BigDecimal unitCost,
                          @RequestParam(required = false) String note,
                          RedirectAttributes redirect) {
        try {
            stockService.receive(productId, quantity, unitCost, note);
            redirect.addFlashAttribute("success", "Stock received.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stock";
    }

    @GetMapping("/stock/periods")
    public String periods(Model model) {
        List<StockPeriod> periods = stockService.periods();
        model.addAttribute("periods", periods);
        return "stock/periods";
    }

    @PostMapping("/stock/periods")
    @PreAuthorize("hasAuthority('STOCK_PERIOD_MANAGE')")
    public String openPeriod(@RequestParam LocalDate start,
                             @RequestParam LocalDate end,
                             @RequestParam(required = false) String notes,
                             RedirectAttributes redirect) {
        try {
            StockPeriod period = stockService.openPeriod(start, end, notes);
            redirect.addFlashAttribute("success", "Stock period opened.");
            return "redirect:/stock/periods/" + period.getId();
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/stock/periods";
        }
    }

    @GetMapping("/stock/periods/{id}")
    public String period(@PathVariable Long id, Model model) {
        model.addAttribute("period", stockService.period(id));
        model.addAttribute("lines", stockService.periodLines(id));
        return "stock/period";
    }

    @PostMapping("/stock/periods/{id}/close")
    @PreAuthorize("hasAuthority('STOCK_PERIOD_MANAGE')")
    public String closePeriod(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            stockService.closePeriod(id);
            redirect.addFlashAttribute("success", "Stock period closed.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stock/periods/" + id;
    }
}
