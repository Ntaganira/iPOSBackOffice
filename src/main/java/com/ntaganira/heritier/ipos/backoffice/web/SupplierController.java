package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.SupplierForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Supplier;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
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

import java.math.BigDecimal;

@Controller
@PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/suppliers")
    public String suppliers(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size,
                            Model model) {
        Page<Supplier> suppliers = supplierService.search(q,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("q", q);
        return "suppliers/list";
    }

    @GetMapping("/suppliers/new")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String createForm(Model model) {
        model.addAttribute("supplierForm", new SupplierForm());
        model.addAttribute("editing", false);
        return "suppliers/form";
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String create(@Valid @ModelAttribute("supplierForm") SupplierForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            return "suppliers/form";
        }
        Supplier supplier = supplierService.create(form);
        redirect.addFlashAttribute("success", "Supplier " + supplier.getName() + " created.");
        return "redirect:/suppliers/" + supplier.getId();
    }

    @GetMapping("/suppliers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.get(id);
        model.addAttribute("supplier", supplier);
        model.addAttribute("outstanding", supplierService.outstanding(supplier));
        model.addAttribute("purchases", supplierService.purchases(id));
        model.addAttribute("payments", supplierService.payments(id));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "suppliers/detail";
    }

    @GetMapping("/suppliers/{id}/edit")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("supplierForm", supplierService.formFor(id));
        model.addAttribute("supplier", supplierService.get(id));
        model.addAttribute("editing", true);
        return "suppliers/form";
    }

    @PostMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("supplierForm") SupplierForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("supplier", supplierService.get(id));
            model.addAttribute("editing", true);
            return "suppliers/form";
        }
        supplierService.update(id, form);
        redirect.addFlashAttribute("success", "Supplier updated.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/suppliers/{id}/toggle")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        supplierService.toggleActive(id);
        redirect.addFlashAttribute("success", "Supplier status updated.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/suppliers/{id}/payments")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String reference,
                                RedirectAttributes redirect) {
        try {
            supplierService.recordPayment(id, amount, method, reference);
            redirect.addFlashAttribute("success", "Payment recorded.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/suppliers/" + id;
    }
}
