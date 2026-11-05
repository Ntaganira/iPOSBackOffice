package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : PasswordChangeFilter.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Forces temporary-password users to change their password.
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

public class PasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/profile",
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof IposUserPrincipal principal
                && principal.passwordChangeRequired()
                && EXEMPT_PREFIXES.stream().noneMatch(path::startsWith)) {
            String target = principal.isSuperAdmin()
                    ? "/admin/profile?passwordChange=required"
                    : "/profile?passwordChange=required";
            response.sendRedirect(request.getContextPath() + target);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
