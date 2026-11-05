package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : BootstrapService.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Creates the initial tenant and platform administrator accounts.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.config.AppProperties;
import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ShopSettingRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BootstrapService {

    private final AppUserRepository userRepository;
    private final ShopSettingRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapService(AppUserRepository userRepository,
                            ShopSettingRepository settingsRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void ensureSuperAdmin(AppProperties.SuperAdmin properties) {
        if (userRepository.findByUsername(properties.username()).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(properties.username());
        user.setPasswordHash(passwordEncoder.encode(properties.password()));
        user.setFullName(properties.fullName());
        user.setEmail(properties.email());
        user.setRole(UserRole.SUPER_ADMIN);
        user.setEnabled(true);
        user.setPasswordChangeRequired(true);
        userRepository.save(user);
    }

    @Transactional
    public void ensureDefaultAdmin(AppProperties.Admin properties) {
        if (userRepository.findByUsername(properties.username()).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(properties.username());
        user.setPasswordHash(passwordEncoder.encode(properties.password()));
        user.setFullName(properties.fullName());
        user.setEmail(StringUtils.hasText(properties.email())
                ? properties.email()
                : properties.username() + "@ipos.local");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void ensureDefaultSettings() {
        if (settingsRepository.findFirstByOrderByIdAsc().isPresent()) {
            return;
        }
        ShopSetting settings = new ShopSetting();
        settings.setShopName("iPOS Shop");
        settings.setCurrency("RWF");
        settingsRepository.save(settings);
    }
}
