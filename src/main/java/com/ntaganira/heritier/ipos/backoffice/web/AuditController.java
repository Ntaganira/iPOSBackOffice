package com.ntaganira.heritier.ipos.backoffice.web;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.web
 * - File      : AuditController.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Web controller for audit.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.dto.AuditLogView;
import com.ntaganira.heritier.ipos.backoffice.repository.AuditLogRepository;
import com.ntaganira.heritier.ipos.backoffice.service.AuditDiffService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditDiffService auditDiffService;

    public AuditController(AuditLogRepository auditLogRepository, AuditDiffService auditDiffService) {
        this.auditLogRepository = auditLogRepository;
        this.auditDiffService = auditDiffService;
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) String username,
                        @RequestParam(required = false) String action,
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        ZoneId zone = ZoneId.systemDefault();
        String usernameFilter = username == null ? "" : username;
        String actionFilter = action == null ? "" : action;
        String entityFilter = entityType == null ? "" : entityType;
        String query = q == null || q.isBlank() ? "" : "%" + q.trim().toLowerCase() + "%";
        Page<AuditLogView> logs = auditLogRepository.search(
                start.atStartOfDay(zone).toInstant(),
                end.plusDays(1).atStartOfDay(zone).toInstant(),
                usernameFilter, actionFilter, entityFilter, query,
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(10, size))))
                .map(auditDiffService::view);
        model.addAttribute("logs", logs);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("username", username);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("q", q);
        model.addAttribute("actions", auditLogRepository.distinctActions());
        model.addAttribute("entityTypes", auditLogRepository.distinctEntityTypes());
        return "audit/index";
    }
}
