package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : LogoutAuditHandler.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Audit handler for user logout events.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LogoutAuditHandler implements LogoutHandler {

    private final AuditService auditService;

    public LogoutAuditHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            auditService.recordAs(null, authentication.getName(), "", "LOGOUT", "Session", null,
                    authentication.getName(), Map.of("success", true));
        }
    }
}
