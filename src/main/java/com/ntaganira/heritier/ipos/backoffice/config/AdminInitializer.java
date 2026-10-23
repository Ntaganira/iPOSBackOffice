package com.ntaganira.heritier.ipos.backoffice.config;

import com.ntaganira.heritier.ipos.backoffice.entity.AppUser;
import com.ntaganira.heritier.ipos.backoffice.enums.UserRole;
import com.ntaganira.heritier.ipos.backoffice.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public AdminInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            AppUser user = new AppUser();
            user.setUsername(properties.admin().username());
            user.setPasswordHash(passwordEncoder.encode(properties.admin().password()));
            user.setFullName(properties.admin().fullName());
            user.setEmail(StringUtils.hasText(properties.admin().email()) ? properties.admin().email() : null);
            user.setRole(UserRole.ADMIN);
            user.setEnabled(true);
            userRepository.save(user);
            log.info("Created initial back-office administrator '{}'.", user.getUsername());
        }
        if ("admin123".equals(properties.admin().password())) {
            log.warn("APP_ADMIN_PASSWORD is still the development default. Change it before exposing this service.");
        }
        if (properties.sync().token().startsWith("ipos-dev-")) {
            log.warn("APP_SYNC_TOKEN is still the development default. Set a long random token before connecting terminals.");
        }
    }
}
