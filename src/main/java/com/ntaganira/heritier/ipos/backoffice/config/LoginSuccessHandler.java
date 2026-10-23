package com.ntaganira.heritier.ipos.backoffice.config;

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.service.AuditService;
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
        userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            auditService.recordAs(user.getId(), user.getUsername(), user.getRole().name(),
                    "LOGIN", "Session", null, user.getUsername(), Map.of("success", true));
            if (user.isPasswordChangeRequired()) {
                setDefaultTargetUrl("/profile?passwordChange=required");
            } else if (user.getRole() == UserRole.CASHIER) {
                setDefaultTargetUrl("/pos");
            } else if (user.getRole() == UserRole.STOREKEEPER) {
                setDefaultTargetUrl("/stock");
            } else {
                setDefaultTargetUrl("/dashboard");
            }
        });
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
