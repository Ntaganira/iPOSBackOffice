package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : PurchaseController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for purchase.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.PurchaseForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Purchase;
import com.ntaganira.heritier.ipos.backoffice.enums.PurchaseStatus;
import com.ntaganira.heritier.ipos.backoffice.service.ProductService;
import com.ntaganira.heritier.ipos.backoffice.service.PurchaseService;
import com.ntaganira.heritier.ipos.backoffice.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('PURCHASE_VIEW')")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final ProductService productService;

    public PurchaseController(PurchaseService purchaseService,
                              SupplierService supplierService,
                              ProductService productService) {
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
        this.productService = productService;
    }

    @GetMapping("/purchases")
    public String purchases(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Long supplierId,
                            @RequestParam(required = false) PurchaseStatus status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size,
                            Model model) {
        Page<Purchase> purchases = purchaseService.search(q, supplierId, status, null, null,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.DESC, "purchaseDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("purchases", purchases);
        model.addAttribute("suppliers", supplierService.active());
        model.addAttribute("statuses", PurchaseStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("status", status);
        model.addAttribute("pendingCount", purchaseService.pendingCount());
        return "purchases/list";
    }

    @GetMapping("/purchases/new")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public String createForm(@RequestParam(required = false) Long supplierId, Model model) {
        PurchaseForm form = new PurchaseForm();
        form.setSupplierId(supplierId);
        model.addAttribute("purchaseForm", form);
        formOptions(model);
        return "purchases/form";
    }

    @PostMapping("/purchases")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public String create(@Valid @ModelAttribute("purchaseForm") PurchaseForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            return "purchases/form";
        }
        try {
            Purchase purchase = purchaseService.create(form);
            redirect.addFlashAttribute("success", "Purchase " + purchase.getPurchaseNumber() + " created.");
            return "redirect:/purchases/" + purchase.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            formOptions(model);
            return "purchases/form";
        }
    }

    @GetMapping("/purchases/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("purchase", purchaseService.get(id));
        return "purchases/detail";
    }

    @PostMapping("/purchases/{id}/receive")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIVE')")
    public String receive(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            purchaseService.receive(id);
            redirect.addFlashAttribute("success", "Purchase received and stock updated.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/purchases/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            purchaseService.cancel(id);
            redirect.addFlashAttribute("success", "Purchase cancelled.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/purchases/" + id;
    }

    private void formOptions(Model model) {
        model.addAttribute("suppliers", supplierService.active());
        model.addAttribute("products", productService.search("", null, true,
                PageRequest.of(0, 500, Sort.by("name"))).getContent());
    }
}
