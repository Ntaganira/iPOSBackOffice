package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : LoginSuccessHandler.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Authentication handler for successful logins.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public LoginSuccessHandler(AppUserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws ServletException, IOException {
        if (authentication.getPrincipal() instanceof IposUserPrincipal principal) {
            if (principal.isSuperAdmin()) {
                TenantContext.runAsSystem(() -> recordLogin(principal));
                String target = principal.passwordChangeRequired()
                        ? "/admin/profile?passwordChange=required"
                        : "/admin";
                response.sendRedirect(request.getContextPath() + target);
                return;
            }
            TenantContext.runAs(principal.tenantId(), () -> recordLogin(principal));
            if (principal.role() == UserRole.CASHIER) {
                setDefaultTargetUrl("/pos");
            } else if (principal.role() == UserRole.STOREKEEPER) {
                setDefaultTargetUrl("/stock");
            } else {
                setDefaultTargetUrl("/dashboard");
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void recordLogin(IposUserPrincipal principal) {
        userRepository.findById(principal.id()).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            auditService.recordAs(user.getId(), user.getUsername(), user.getRole().name(),
                    "LOGIN", "Session", null, user.getUsername(), Map.of("success", true));
            if (user.isPasswordChangeRequired()) {
                setDefaultTargetUrl("/profile?passwordChange=required");
            }
        });
    }
}
