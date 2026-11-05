package com.ntaganira.heritier.ipos.backoffice.tenant;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.tenant
 * - File      : TenantIdentifierResolver.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Supplies the current tenant identifier to Hibernate.
 * </pre>
 */

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.get();
        return tenantId == null ? TenantContext.SYSTEM_TENANT_ID : tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(Long tenantId) {
        return Long.valueOf(TenantContext.SYSTEM_TENANT_ID).equals(tenantId);
    }
}
