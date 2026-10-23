package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.dto.ExpenseForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Expense;
import com.ntaganira.heritier.ipos.backoffice.enums.ExpenseStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.PaymentMethod;
import com.ntaganira.heritier.ipos.backoffice.enums.Recurrence;
import com.ntaganira.heritier.ipos.backoffice.service.ExpenseService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@PreAuthorize("hasAuthority('EXPENSE_VIEW')")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/expenses")
    public String expenses(@RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) PaymentMethod method,
                           @RequestParam(required = false) ExpenseStatus status,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "30") int size,
                           Model model) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        Page<Expense> expenses = expenseService.search(start, end, categoryId, method, status, q,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("expenses", expenses);
        model.addAttribute("total", expenseService.total(start, end, categoryId, method, status, q));
        model.addAttribute("categories", expenseService.categories());
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("method", method);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        return "expenses/list";
    }

    @GetMapping("/expenses/new")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("expenseForm", new ExpenseForm());
        formOptions(model);
        model.addAttribute("editing", false);
        return "expenses/form";
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public String create(@Valid @ModelAttribute("expenseForm") ExpenseForm form,
                         BindingResult binding,
                         @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            formOptions(model);
            model.addAttribute("editing", false);
            return "expenses/form";
        }
        Expense expense = expenseService.create(form, attachment);
        redirect.addFlashAttribute("success", "Expense saved.");
        return "redirect:/expenses/" + expense.getId();
    }

    @GetMapping("/expenses/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("expense", expenseService.get(id));
        return "expenses/detail";
    }

    @GetMapping("/expenses/{id}/edit")
    @PreAuthorize("hasAuthority('EXPENSE_EDIT')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("expenseForm", expenseService.formFor(id));
        model.addAttribute("expense", expenseService.get(id));
        formOptions(model);
        model.addAttribute("editing", true);
        return "expenses/form";
    }

    @PostMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_EDIT')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("expenseForm") ExpenseForm form,
                         BindingResult binding,
                         @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("expense", expenseService.get(id));
            formOptions(model);
            model.addAttribute("editing", true);
            return "expenses/form";
        }
        expenseService.update(id, form, attachment);
        redirect.addFlashAttribute("success", "Expense updated.");
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/expenses/{id}/approve")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            expenseService.approve(id);
            redirect.addFlashAttribute("success", "Expense approved.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/expenses/{id}/reject")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public String reject(@PathVariable Long id, @RequestParam String reason, RedirectAttributes redirect) {
        try {
            expenseService.reject(id, reason);
            redirect.addFlashAttribute("success", "Expense rejected.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/expenses/{id}/pay")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public String pay(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            expenseService.markPaid(id);
            redirect.addFlashAttribute("success", "Expense marked as paid.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/expenses/{id}/void")
    @PreAuthorize("hasAuthority('EXPENSE_VOID')")
    public String voidExpense(@PathVariable Long id, @RequestParam String reason, RedirectAttributes redirect) {
        try {
            expenseService.voidExpense(id, reason);
            redirect.addFlashAttribute("success", "Expense voided.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @GetMapping("/expenses/categories")
    @PreAuthorize("hasAuthority('EXPENSE_CATEGORY_MANAGE')")
    public String categories(Model model) {
        model.addAttribute("categories", expenseService.categories());
        return "expenses/categories";
    }

    @PostMapping("/expenses/categories")
    @PreAuthorize("hasAuthority('EXPENSE_CATEGORY_MANAGE')")
    public String saveCategory(@RequestParam(required = false) Long id,
                               @RequestParam String name,
                               @RequestParam(defaultValue = "false") boolean active,
                               RedirectAttributes redirect) {
        expenseService.saveCategory(id, name, active);
        redirect.addFlashAttribute("success", "Expense category saved.");
        return "redirect:/expenses/categories";
    }

    private void formOptions(Model model) {
        model.addAttribute("categories", expenseService.activeCategories());
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("recurrences", Recurrence.values());
    }
}
