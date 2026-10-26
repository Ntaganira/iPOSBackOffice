package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : SettingsController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for settings.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.EmailLog;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.PrintFormat;
import com.ntaganira.heritier.ipos.backoffice.enums.SmtpSecurity;
import com.ntaganira.heritier.ipos.backoffice.service.EmailService;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
public class SettingsController {

    private final SettingsService settingsService;
    private final EmailService emailService;

    public SettingsController(SettingsService settingsService, EmailService emailService) {
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    @GetMapping("/settings")
    public String settings() {
        return "redirect:/settings/shop";
    }

    @GetMapping("/settings/shop")
    public String shop(Model model) {
        model.addAttribute("settings", settingsService.get());
        model.addAttribute("printFormats", PrintFormat.values());
        return "settings/shop";
    }

    @PostMapping("/settings/shop")
    public String updateShop(@ModelAttribute("settings") ShopSetting form,
                             @RequestPart(value = "logo", required = false) MultipartFile logo,
                             RedirectAttributes redirect) {
        try {
            settingsService.updateGeneral(form);
            if (logo != null && !logo.isEmpty()) {
                settingsService.updateLogo(logo);
            }
            redirect.addFlashAttribute("success", "Shop settings saved.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings/shop";
    }

    @GetMapping("/settings/email")
    @PreAuthorize("hasAuthority('EMAIL_SETTINGS_MANAGE')")
    public String email(Model model) {
        model.addAttribute("settings", settingsService.get());
        model.addAttribute("securityOptions", SmtpSecurity.values());
        return "settings/email";
    }

    @PostMapping("/settings/email")
    @PreAuthorize("hasAuthority('EMAIL_SETTINGS_MANAGE')")
    public String updateEmail(@ModelAttribute("settings") ShopSetting form,
                              RedirectAttributes redirect) {
        try {
            settingsService.updateEmailSettings(form);
            redirect.addFlashAttribute("success", "Email settings saved.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings/email";
    }

    @PostMapping("/settings/email/test")
    @PreAuthorize("hasAuthority('EMAIL_SETTINGS_MANAGE')")
    public String testEmail(@RequestParam String recipient, RedirectAttributes redirect) {
        try {
            EmailLog log = emailService.sendTest(recipient);
            if (log.getStatus() == EmailStatus.SENT) {
                redirect.addFlashAttribute("success", "Test email sent to " + recipient + ".");
            } else {
                redirect.addFlashAttribute("error", "Test email failed: " + log.getErrorMessage());
            }
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", "Test email failed: " + ex.getMessage());
        }
        return "redirect:/settings/email";
    }

    @GetMapping("/settings/tax")
    public String tax(Model model) {
        model.addAttribute("taxCategories", settingsService.taxCategories());
        return "settings/tax";
    }

    @PostMapping("/settings/tax")
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
        return "redirect:/settings/tax";
    }
}
