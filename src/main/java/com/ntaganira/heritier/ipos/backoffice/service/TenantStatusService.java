package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : TenantStatusService.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Cached tenant availability checks used by request filters and login.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantStatusService {

    private final TenantRepository tenantRepository;
    private final ConcurrentHashMap<Long, Boolean> activeCache = new ConcurrentHashMap<>();

    public TenantStatusService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public boolean isActive(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        if (tenantId == TenantContext.SYSTEM_TENANT_ID) {
            return true;
        }
        return activeCache.computeIfAbsent(tenantId, id ->
                tenantRepository.findById(id).map(Tenant::isEnabled).orElse(false));
    }

    public boolean isActiveByCode(String code) {
        return tenantRepository.findByCodeIgnoreCase(code)
                .map(tenant -> isActive(tenant.getId()))
                .orElse(false);
    }

    public void evict(Long tenantId) {
        activeCache.remove(tenantId);
    }
}
