package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : LoginFailureHandler.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Authentication handler for failed login attempts.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuditService auditService;
    private final AppUserRepository userRepository;

    public LoginFailureHandler(AuditService auditService, AppUserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase();
            AppUser user = userRepository.findGlobalByEmail(normalized).orElse(null);
            long tenantId = user == null ? TenantContext.SYSTEM_TENANT_ID : user.getTenantId();
            TenantContext.runAs(tenantId, () -> auditService.recordAs(
                    user == null ? null : user.getId(),
                    normalized,
                    user == null ? "" : user.getRole().name(),
                    "LOGIN_FAILED", "Session", null, normalized,
                    Map.of("reason", exception.getMessage() == null ? "" : exception.getMessage())));
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
