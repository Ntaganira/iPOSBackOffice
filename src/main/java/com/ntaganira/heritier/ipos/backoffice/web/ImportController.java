package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : ImportController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for import.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.ImportForm;
import com.ntaganira.heritier.ipos.backoffice.entity.ImportBatch;
import com.ntaganira.heritier.ipos.backoffice.enums.ImportStatus;
import com.ntaganira.heritier.ipos.backoffice.service.ImportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@PreAuthorize("hasAuthority('IMPORT_VIEW')")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/imports")
    public String imports(@RequestParam(required = false) String q,
                          @RequestParam(required = false) ImportStatus status,
                          @RequestParam(required = false) LocalDate from,
                          @RequestParam(required = false) LocalDate to,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "25") int size,
                          Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusMonths(3) : from;
        if (start.isAfter(end)) {
            start = end;
        }
        Page<ImportBatch> imports = importService.search(q, status, start, end,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.DESC, "importDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("imports", imports);
        model.addAttribute("statuses", ImportStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "imports/list";
    }

    @GetMapping("/imports/new")
    @PreAuthorize("hasAuthority('IMPORT_MANAGE')")
    public String createForm(Model model) {
        model.addAttribute("importForm", new ImportForm());
        formOptions(model);
        return "imports/form";
    }

    @PostMapping("/imports")
    @PreAuthorize("hasAuthority('IMPORT_MANAGE')")
    public String create(@ModelAttribute("importForm") ImportForm form, Model model,
                         RedirectAttributes redirect) {
        try {
            ImportBatch batch = importService.create(form);
            redirect.addFlashAttribute("success", "Import " + batch.getReferenceNo() + " created.");
            return "redirect:/imports/" + batch.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            formOptions(model);
            return "imports/form";
        }
    }

    @GetMapping("/imports/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("importBatch", importService.get(id));
        return "imports/detail";
    }

    @GetMapping("/imports/{id}/edit")
    @PreAuthorize("hasAuthority('IMPORT_MANAGE')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        ImportBatch batch = importService.get(id);
        if (!batch.isDraft()) {
            redirect.addFlashAttribute("error", "Only draft imports can be edited.");
            return "redirect:/imports/" + id;
        }
        model.addAttribute("importForm", ImportForm.from(batch));
        formOptions(model);
        return "imports/form";
    }

    @PostMapping("/imports/{id}")
    @PreAuthorize("hasAuthority('IMPORT_MANAGE')")
    public String update(@PathVariable Long id, @ModelAttribute("importForm") ImportForm form,
                         Model model, RedirectAttributes redirect) {
        try {
            ImportBatch batch = importService.update(id, form);
            redirect.addFlashAttribute("success", "Import " + batch.getReferenceNo() + " updated.");
            return "redirect:/imports/" + batch.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("importForm", form);
            formOptions(model);
            return "imports/form";
        }
    }

    @PostMapping("/imports/{id}/receive")
    @PreAuthorize("hasAuthority('IMPORT_RECEIVE')")
    public String receive(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            ImportBatch batch = importService.receive(id);
            redirect.addFlashAttribute("success",
                    "Import " + batch.getReferenceNo() + " received and stock updated.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/imports/" + id;
    }

    @PostMapping("/imports/{id}/cancel")
    @PreAuthorize("hasAuthority('IMPORT_MANAGE')")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            importService.cancel(id);
            redirect.addFlashAttribute("success", "Import cancelled.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/imports/" + id;
    }

    private void formOptions(Model model) {
        model.addAttribute("suppliers", importService.suppliers());
        model.addAttribute("products", importService.products());
        model.addAttribute("currencies", new String[]{"USD", "EUR", "CNY", "GBP", "KES", "RWF"});
    }
}
