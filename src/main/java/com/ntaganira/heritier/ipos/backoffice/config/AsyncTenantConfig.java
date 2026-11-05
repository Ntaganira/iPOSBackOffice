package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : AsyncTenantConfig.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Propagates the tenant context to asynchronous tasks.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration
public class AsyncTenantConfig {

    @Bean
    public TaskDecorator tenantTaskDecorator() {
        return runnable -> {
            Long tenantId = TenantContext.get();
            return () -> {
                Long previous = TenantContext.get();
                TenantContext.set(tenantId);
                try {
                    runnable.run();
                } finally {
                    TenantContext.set(previous);
                }
            };
        };
    }
}
