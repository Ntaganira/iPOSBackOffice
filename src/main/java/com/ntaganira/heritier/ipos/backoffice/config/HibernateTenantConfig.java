package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : HibernateTenantConfig.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Registers the tenant identifier resolver with Hibernate.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierCustomizer(TenantIdentifierResolver resolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
