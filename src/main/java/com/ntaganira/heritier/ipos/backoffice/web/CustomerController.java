package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.CustomerForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Customer;
import com.ntaganira.heritier.ipos.backoffice.enums.CustomerType;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.service.CustomerService;
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
@PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size,
                            Model model) {
        Page<Customer> customers = customerService.search(q,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("customers", customers);
        model.addAttribute("q", q);
        model.addAttribute("totalOutstanding", customerService.totalOutstanding());
        return "customers/list";
    }

    @GetMapping("/customers/new")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm());
        formOptions(model);
        model.addAttribute("editing", false);
        return "customers/form";
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public String create(@Valid @ModelAttribute("customerForm") CustomerForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            model.addAttribute("editing", false);
            return "customers/form";
        }
        Customer customer = customerService.create(form);
        redirect.addFlashAttribute("success", "Customer " + customer.getName() + " created.");
        return "redirect:/customers/" + customer.getId();
    }

    @GetMapping("/customers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customerService.get(id);
        model.addAttribute("customer", customer);
        model.addAttribute("outstanding", customerService.outstanding(customer));
        model.addAttribute("creditSales", customerService.creditSales(id));
        model.addAttribute("payments", customerService.payments(id));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "customers/detail";
    }

    @GetMapping("/customers/{id}/edit")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("customerForm", customerService.formFor(id));
        model.addAttribute("customer", customerService.get(id));
        formOptions(model);
        model.addAttribute("editing", true);
        return "customers/form";
    }

    @PostMapping("/customers/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("customerForm") CustomerForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("customer", customerService.get(id));
            formOptions(model);
            model.addAttribute("editing", true);
            return "customers/form";
        }
        customerService.update(id, form);
        redirect.addFlashAttribute("success", "Customer updated.");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/customers/{id}/toggle")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        customerService.toggleActive(id);
        redirect.addFlashAttribute("success", "Customer status updated.");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/customers/{id}/payments")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String reference,
                                RedirectAttributes redirect) {
        try {
            customerService.recordPayment(id, amount, method, reference);
            redirect.addFlashAttribute("success", "Payment recorded.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    private void formOptions(Model model) {
        model.addAttribute("customerTypes", CustomerType.values());
    }
}
