package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : TenantContextFilter.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Binds the authenticated user's tenant to each request.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import com.ntaganira.heritier.ipos.backoffice.service.TenantStatusService;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantStatusService tenantStatusService;

    public TenantContextFilter(TenantStatusService tenantStatusService) {
        this.tenantStatusService = tenantStatusService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Long previous = TenantContext.get();
        TenantContext.clear();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof IposUserPrincipal principal) {
                if (principal.isSuperAdmin()) {
                    TenantContext.setSystem();
                } else if (!tenantStatusService.isActive(principal.tenantId())) {
                    SecurityContextHolder.clearContext();
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    response.sendRedirect(request.getContextPath() + "/login?tenantDisabled");
                    return;
                } else {
                    TenantContext.set(principal.tenantId());
                }
            } else {
                TenantContext.set(TenantContext.DEFAULT_TENANT_ID);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.set(previous);
        }
    }
}
