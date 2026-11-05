package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : AdminInitializer.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Creates the initial administrator account at startup.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.service.BootstrapService;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final BootstrapService bootstrapService;
    private final AppProperties properties;

    public AdminInitializer(BootstrapService bootstrapService, AppProperties properties) {
        this.bootstrapService = bootstrapService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        TenantContext.runAsSystem(() -> bootstrapService.ensureSuperAdmin(properties.superAdmin()));
        TenantContext.runAs(TenantContext.DEFAULT_TENANT_ID, () -> {
            bootstrapService.ensureDefaultAdmin(properties.admin());
            bootstrapService.ensureDefaultSettings();
        });
        log.info("Ensured platform and default tenant administrator accounts exist.");
        if ("admin123".equals(properties.admin().password())) {
            log.warn("APP_ADMIN_PASSWORD is still the development default. Change it before exposing this service.");
        }
        if ("superadmin123".equals(properties.superAdmin().password())) {
            log.warn("IPOS_SUPER_ADMIN_PASSWORD is still the development default. Change it before exposing this service.");
        }
        if (properties.sync().token().startsWith("ipos-dev-")) {
            log.warn("APP_SYNC_TOKEN is still the development default. Set a long random token before connecting terminals.");
        }
    }
}
