package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : SuperAdminRedirectFilter.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Keeps super administrators inside the platform administration area.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class SuperAdminRedirectFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/admin",
            "/css/",
            "/js/",
            "/images/",
            "/files/",
            "/vendor/",
            "/actuator/",
            "/favicon.ico",
            "/login",
            "/logout",
            "/403",
            "/error"
    );

    private static final Set<String> PASSWORD_CHANGE_EXEMPT_PREFIXES = Set.of(
            "/admin/profile",
            "/css/",
            "/js/",
            "/images/",
            "/files/",
            "/vendor/",
            "/actuator/",
            "/favicon.ico",
            "/login",
            "/logout",
            "/403",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof IposUserPrincipal principal
                && principal.isSuperAdmin()
                && principal.passwordChangeRequired()
                && PASSWORD_CHANGE_EXEMPT_PREFIXES.stream().noneMatch(path::startsWith)) {
            response.sendRedirect(request.getContextPath() + "/admin/profile?passwordChange=required");
            return;
        }
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof IposUserPrincipal principal
                && principal.isSuperAdmin()
                && ALLOWED_PREFIXES.stream().noneMatch(path::startsWith)) {
            response.sendRedirect(request.getContextPath() + "/admin");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
