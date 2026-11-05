package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : AdminController.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Platform administration pages for super administrators.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.AdminUserView;
import com.ntaganira.heritier.ipos.backoffice.dto.AuditLogView;
import com.ntaganira.heritier.ipos.backoffice.dto.TenantForm;
import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import com.ntaganira.heritier.ipos.backoffice.service.AdminAuditService;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import com.ntaganira.heritier.ipos.backoffice.service.TenantService;
import com.ntaganira.heritier.ipos.backoffice.service.TenantUsageService;
import com.ntaganira.heritier.ipos.backoffice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.time.LocalDate;
import java.util.List;

@Controller
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final TenantUsageService tenantUsageService;
    private final AdminAuditService adminAuditService;
    private final SettingsService settingsService;
    private final UserService userService;

    public AdminController(TenantService tenantService,
                           TenantRepository tenantRepository,
                           TenantUsageService tenantUsageService,
                           AdminAuditService adminAuditService,
                           SettingsService settingsService,
                           UserService userService) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.tenantUsageService = tenantUsageService;
        this.adminAuditService = adminAuditService;
        this.settingsService = settingsService;
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("tenantCount", tenantUsageService.totalTenants());
        model.addAttribute("activeTenantCount", tenantUsageService.activeTenants());
        model.addAttribute("userCount", tenantUsageService.totalUsers());
        model.addAttribute("saleCount", tenantUsageService.totalSales());
        model.addAttribute("tenants", tenantRepository.findAllByOrderByNameAsc());
        Page<AuditLogView> recent = adminAuditService.search(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null,
                PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "occurredAt")));
        model.addAttribute("recentAudit", recent.getContent());
        return "admin/dashboard";
    }

    @GetMapping("/admin/tenants")
    public String tenants(@RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "25") int size,
                          Model model) {
        model.addAttribute("tenants", tenantService.search(q,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(5, size)),
                        Sort.by(Sort.Direction.ASC, "name"))));
        model.addAttribute("q", q);
        return "admin/tenants";
    }

    @GetMapping("/admin/tenants/new")
    public String newTenant(Model model) {
        model.addAttribute("tenantForm", new TenantForm());
        return "admin/tenant-form";
    }

    @PostMapping("/admin/tenants")
    public String createTenant(@Valid @ModelAttribute("tenantForm") TenantForm form,
                               BindingResult binding,
                               Model model,
                               RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return "admin/tenant-form";
        }
        try {
            Tenant tenant = tenantService.create(form);
            redirect.addFlashAttribute("success", "Tenant " + tenant.getName() + " created.");
            return "redirect:/admin/tenants/" + tenant.getId();
        } catch (BusinessException ex) {
            model.addAttribute("formError", ex.getMessage());
            return "admin/tenant-form";
        }
    }

    @GetMapping("/admin/tenants/{id}")
    public String tenant(@PathVariable Long id, Model model) {
        Tenant tenant = tenantService.get(id);
        List<AdminUserView> users = tenantService.users(id);
        model.addAttribute("tenant", tenant);
        model.addAttribute("users", users);
        model.addAttribute("usage", tenantUsageService.counts(id));
        return "admin/tenant-detail";
    }

    @PostMapping("/admin/tenants/{id}/toggle")
    public String toggleTenant(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            tenantService.toggle(id);
            redirect.addFlashAttribute("success", "Tenant status updated.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/tenants/" + id;
    }

    @GetMapping("/admin/audit")
    public String audit(@RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String tenant,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        Page<AuditLogView> logs = adminAuditService.search(start, end, q, tenant,
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(10, size))));
        model.addAttribute("logs", logs);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("q", q);
        model.addAttribute("tenant", tenant);
        model.addAttribute("tenants", tenantRepository.findAllByOrderByNameAsc());
        return "admin/audit";
    }

    @GetMapping("/admin/tax")
    public String tax(Model model) {
        model.addAttribute("taxCategories", settingsService.taxCategories());
        return "admin/tax";
    }

    @PostMapping("/admin/tax")
    public String updateTax(@RequestParam String code,
                            @RequestParam String name,
                            @RequestParam BigDecimal rate,
                            @RequestParam(defaultValue = "false") boolean exempt,
                            @RequestParam(defaultValue = "false") boolean active,
                            RedirectAttributes redirect) {
        try {
            settingsService.updateTaxCategory(code, name, rate, exempt, active);
            redirect.addFlashAttribute("success", "Tax category updated.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/tax";
    }

    @GetMapping("/admin/profile")
    public String profile(@AuthenticationPrincipal IposUserPrincipal principal,
                          @RequestParam(required = false) String passwordChange,
                          Model model) {
        model.addAttribute("principal", principal);
        model.addAttribute("passwordChangeRequired", principal != null && principal.passwordChangeRequired());
        model.addAttribute("passwordChange", passwordChange);
        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String changePassword(@AuthenticationPrincipal IposUserPrincipal principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirect) {
        try {
            userService.changePassword(principal.id(), currentPassword, newPassword, confirmPassword);
            IposUserPrincipal updated = principal.passwordChanged();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    updated, updated.password(), updated.getAuthorities()));
            redirect.addFlashAttribute("success", "Password changed.");
            return "redirect:/admin";
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/profile";
        }
    }
}
