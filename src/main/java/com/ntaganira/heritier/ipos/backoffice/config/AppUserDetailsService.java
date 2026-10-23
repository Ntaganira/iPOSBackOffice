package com.ntaganira.heritier.ipos.backoffice.config;

import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.service.RoleService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final RoleService roleService;

    public AppUserDetailsService(AppUserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    for (String permission : roleService.permissionCodes(user.getRole())) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                    return User.withUsername(user.getUsername())
                            .password(user.getPasswordHash())
                            .authorities(authorities)
                            .disabled(!user.isEnabled())
                            .accountLocked(user.isLocked())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user."));
    }
}
