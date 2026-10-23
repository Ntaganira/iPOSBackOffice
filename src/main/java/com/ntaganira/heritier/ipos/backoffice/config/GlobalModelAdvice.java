package com.ntaganira.heritier.ipos.backoffice.config;

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;
import java.util.Locale;

@ControllerAdvice
public class GlobalModelAdvice {

    private final AppUserRepository userRepository;
    private final SettingsService settingsService;

    public GlobalModelAdvice(AppUserRepository userRepository, SettingsService settingsService) {
        this.userRepository = userRepository;
        this.settingsService = settingsService;
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now().getValue();
    }

    @ModelAttribute("currentUser")
    public AppUser currentUserModel(Authentication authentication) {
        return currentUser(authentication);
    }

    @ModelAttribute("profileFullName")
    public String profileFullName(Authentication authentication) {
        AppUser user = currentUser(authentication);
        return user == null ? "" : user.getFullName();
    }

    @ModelAttribute("shop")
    public Object shop() {
        return settingsService.get();
    }

    @ModelAttribute("profileInitials")
    public String profileInitials(Authentication authentication) {
        AppUser user = currentUser(authentication);
        if (user == null || user.getFullName() == null) {
            return "";
        }
        String[] parts = user.getFullName().trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    @ModelAttribute("profileRole")
    public String profileRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .map(authority -> authority.substring(5))
                .map(role -> role.charAt(0) + role.substring(1).toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
