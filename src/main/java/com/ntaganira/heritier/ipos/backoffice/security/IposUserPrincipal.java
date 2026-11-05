package com.ntaganira.heritier.ipos.backoffice.security;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.security
 * - File      : IposUserPrincipal.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Authenticated principal carrying the user's tenant and role.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.List;

public record IposUserPrincipal(
        Long id,
        String username,
        String email,
        String password,
        String fullName,
        UserRole role,
        Long tenantId,
        String tenantName,
        boolean enabled,
        boolean locked,
        boolean passwordChangeRequired,
        List<GrantedAuthority> authorities
) implements UserDetails, Serializable {

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return authorities == null ? List.of() : authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN || Long.valueOf(0L).equals(tenantId);
    }

    public IposUserPrincipal passwordChanged() {
        return new IposUserPrincipal(id, username, email, password, fullName, role, tenantId,
                tenantName, enabled, locked, false, authorities);
    }
}
