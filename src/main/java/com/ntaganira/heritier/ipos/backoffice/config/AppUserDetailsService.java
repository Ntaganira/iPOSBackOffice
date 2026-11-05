package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : AppUserDetailsService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Loads back-office users for Spring Security.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.security.IposUserPrincipal;
import com.ntaganira.heritier.ipos.backoffice.service.RoleService;
import com.ntaganira.heritier.ipos.backoffice.service.TenantStatusService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantStatusService tenantStatusService;
    private final RoleService roleService;

    public AppUserDetailsService(AppUserRepository userRepository,
                                 TenantRepository tenantRepository,
                                 TenantStatusService tenantStatusService,
                                 RoleService roleService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantStatusService = tenantStatusService;
        this.roleService = roleService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findGlobalByEmail(username)
                .map(user -> {
                    if (!tenantStatusService.isActive(user.getTenantId())) {
                        throw new DisabledException("Tenant is disabled.");
                    }
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    if (user.getRole() != UserRole.SUPER_ADMIN) {
                        for (String permission : roleService.permissionCodes(user.getRole())) {
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }
                    }
                    String tenantName = tenantRepository.findById(user.getTenantId())
                            .map(Tenant::getName)
                            .orElse("System");
                    return new IposUserPrincipal(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getPasswordHash(),
                            user.getFullName(),
                            user.getRole(),
                            user.getTenantId(),
                            tenantName,
                            user.isEnabled(),
                            user.isLocked(),
                            user.isPasswordChangeRequired(),
                            authorities);
                })
                .orElseThrow(() -> new UsernameNotFoundException("Unknown email."));
    }
}
