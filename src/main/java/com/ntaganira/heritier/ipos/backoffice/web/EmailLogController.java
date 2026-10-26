package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : EmailLogController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for email log.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.EmailLog;
import com.ntaganira.heritier.ipos.backoffice.enums.EmailStatus;
import com.ntaganira.heritier.ipos.backoffice.service.BusinessException;
import com.ntaganira.heritier.ipos.backoffice.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('EMAIL_LOG_VIEW')")
public class EmailLogController {

    private final EmailService emailService;

    public EmailLogController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/email-logs")
    public String logs(@RequestParam(required = false) String q,
                       @RequestParam(required = false) EmailStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "50") int size,
                       Model model) {
        Page<EmailLog> logs = emailService.search(q, status,
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(10, size))));
        model.addAttribute("logs", logs);
        model.addAttribute("statuses", EmailStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("failedCount", emailService.failedCount());
        return "email-logs/index";
    }

    @GetMapping("/email-logs/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("log", emailService.get(id));
        return "email-logs/detail";
    }

    @PostMapping("/email-logs/{id}/resend")
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    public String resend(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            EmailLog log = emailService.resend(id);
            if (log.getStatus() == EmailStatus.SENT) {
                redirect.addFlashAttribute("success", "Email resent.");
            } else {
                redirect.addFlashAttribute("error", "Resend failed: " + log.getErrorMessage());
            }
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/email-logs";
    }
}
