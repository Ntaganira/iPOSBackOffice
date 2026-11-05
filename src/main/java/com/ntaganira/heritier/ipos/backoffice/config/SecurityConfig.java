package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : SecurityConfig.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Spring Security rules, login flow and access control.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.service.TenantStatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            LogoutAuditHandler logoutAuditHandler,
            TenantStatusService tenantStatusService,
            @Value("${app.security.remember-me-key}") String rememberMeKey
    ) throws Exception {
        TenantContextFilter tenantContextFilter = new TenantContextFilter(tenantStatusService);
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/files/**",
                                "/vendor/**",
                                "/login",
                                "/favicon.ico",
                                "/actuator/health",
                                "/error",
                                "/403"
                        ).permitAll()
                        .requestMatchers("/sync/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/devices/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tenantContextFilter, AuthorizationFilter.class)
                .addFilterAfter(new SuperAdminRedirectFilter(), TenantContextFilter.class)
                .addFilterAfter(new PasswordChangeFilter(), SuperAdminRedirectFilter.class)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/sync/**"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .addLogoutHandler(logoutAuditHandler)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "ipos-remember-me")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("ipos-remember-me")
                        .tokenValiditySeconds(14 * 24 * 60 * 60))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .invalidSessionUrl("/login?expired")
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler()))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(contentType -> {
                        })
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                );
        return http.build();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                response.sendRedirect(request.getContextPath() + "/403");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
