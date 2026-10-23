package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.entity.Sale;
import com.ntaganira.heritier.ipos.backoffice.entity.SaleLine;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.SaleType;
import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
import com.ntaganira.heritier.ipos.backoffice.service.EmailService;
import com.ntaganira.heritier.ipos.backoffice.service.PdfService;
import com.ntaganira.heritier.ipos.backoffice.service.ReceiptService;
import com.ntaganira.heritier.ipos.backoffice.service.SaleService;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import com.ntaganira.heritier.ipos.backoffice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAuthority('SALE_VIEW')")
public class SalesController {

    private final SaleService saleService;
    private final ReceiptService receiptService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final UserService userService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    public SalesController(SaleService saleService,
                           ReceiptService receiptService,
                           PdfService pdfService,
                           EmailService emailService,
                           UserService userService,
                           SettingsService settingsService,
                           AuditService auditService) {
        this.saleService = saleService;
        this.receiptService = receiptService;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.userService = userService;
        this.settingsService = settingsService;
        this.auditService = auditService;
    }

    @GetMapping("/sales")
    public String sales(@RequestParam(required = false) String q,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) Long cashierId,
                        @RequestParam(required = false) SaleStatus status,
                        @RequestParam(required = false) SaleType type,
                        @RequestParam(required = false) PaymentMethod method,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {
        Page<Sale> sales = saleService.search(
                start(from), end(to), cashierId, null, status, type, q, method,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.DESC, "saleDate")));
        model.addAttribute("sales", sales);
        model.addAttribute("cashiers", userService.activeUsers());
        model.addAttribute("statuses", SaleStatus.values());
        model.addAttribute("types", SaleType.values());
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("cashierId", cashierId);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("method", method);
        model.addAttribute("settings", settingsService.get());
        return "sales/list";
    }

    @GetMapping("/sales/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Sale sale = saleService.sale(id);
        model.addAttribute("sale", sale);
        model.addAttribute("verified", saleService.verifySignature(sale));
        model.addAttribute("settings", settingsService.get());
        return "sales/detail";
    }

    @GetMapping("/sales/{id}/receipt")
    public String receipt(@PathVariable Long id,
                          @RequestParam(defaultValue = "false") boolean embedded,
                          @RequestParam(defaultValue = "false") boolean copy,
                          Model model) {
        Sale sale = saleService.sale(id);
        if (copy) {
            auditService.record("REPRINT", "Sale", sale.getId(), sale.getReceiptNumber(),
                    null, auditService.snapshot("receiptNumber", sale.getReceiptNumber()));
        }
        model.addAttribute("sale", sale);
        model.addAttribute("shop", settingsService.get());
        model.addAttribute("qr", receiptService.qrDataUri(sale));
        model.addAttribute("receiptDateTime", receiptService.dateTime(sale));
        model.addAttribute("verificationCode", receiptService.verificationCode(sale));
        model.addAttribute("verified", saleService.verifySignature(sale));
        model.addAttribute("copy", copy);
        model.addAttribute("embedded", embedded);
        model.addAttribute("defaultFormat", settingsService.get().getDefaultPrintFormat().name());
        model.addAttribute("taxLines", taxLines(sale));
        return "sales/receipt";
    }

    @GetMapping("/sales/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,
                                      @RequestParam(defaultValue = "invoice") String format) {
        Sale sale = saleService.sale(id);
        byte[] content = "receipt".equalsIgnoreCase(format) ? pdfService.receipt(sale) : pdfService.invoice(sale);
        String filename = ("receipt".equalsIgnoreCase(format) ? "Receipt-" : "Invoice-") + sale.getReceiptNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @PostMapping("/sales/{id}/refund")
    @PreAuthorize("hasAuthority('SALE_REFUND')")
    public String refund(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        Sale refund = saleService.refund(id, reason);
        redirect.addFlashAttribute("success", "Refund " + refund.getReceiptNumber() + " created.");
        return "redirect:/sales/" + refund.getId() + "/receipt";
    }

    @PostMapping("/sales/{id}/email")
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    public String email(@PathVariable Long id,
                        @RequestParam String recipient,
                        RedirectAttributes redirect) {
        emailService.sendReceipt(id, recipient, null);
        redirect.addFlashAttribute("success", "Receipt email queued for " + recipient + ".");
        return "redirect:/sales/" + id;
    }

    private List<Map<String, Object>> taxLines(Sale sale) {
        Map<String, BigDecimal[]> grouped = new LinkedHashMap<>();
        for (SaleLine line : sale.getLines()) {
            String code = line.getTaxCategoryCode() == null ? "A" : line.getTaxCategoryCode();
            BigDecimal tax = line.getTaxAmount() == null ? BigDecimal.ZERO : line.getTaxAmount();
            BigDecimal taxable = line.getLineTotal().subtract(tax);
            BigDecimal[] totals = grouped.computeIfAbsent(code, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[0] = totals[0].add(taxable);
            totals[1] = totals[1].add(tax);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.forEach((code, totals) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", code);
            row.put("taxable", totals[0]);
            row.put("tax", totals[1]);
            rows.add(row);
        });
        return rows;
    }

    private Instant start(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant end(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
