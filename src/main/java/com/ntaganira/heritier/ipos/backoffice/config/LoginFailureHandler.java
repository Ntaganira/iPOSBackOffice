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

import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
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

    public LoginFailureHandler(AuditService auditService) {
        this.auditService = auditService;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            auditService.recordAs(null, username.trim(), "", "LOGIN_FAILED", "Session", null,
                    username.trim(), Map.of("reason", exception.getMessage() == null ? "" : exception.getMessage()));
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
