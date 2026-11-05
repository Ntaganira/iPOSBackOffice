package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : RecurringExpenseScheduler.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Materializes recurring expenses for every active tenant.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.Tenant;
import com.ntaganira.heritier.ipos.backoffice.repository.TenantRepository;
import com.ntaganira.heritier.ipos.backoffice.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringExpenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseScheduler.class);

    private final TenantRepository tenantRepository;
    private final ExpenseService expenseService;

    public RecurringExpenseScheduler(TenantRepository tenantRepository, ExpenseService expenseService) {
        this.tenantRepository = tenantRepository;
        this.expenseService = expenseService;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void materializeAllTenants() {
        for (Tenant tenant : tenantRepository.findByEnabledTrueOrderByNameAsc()) {
            if (tenant.getId() == null || tenant.getId() == TenantContext.SYSTEM_TENANT_ID) {
                continue;
            }
            try {
                TenantContext.runAs(tenant.getId(), expenseService::materializeRecurringForCurrentTenant);
            } catch (RuntimeException ex) {
                log.warn("Recurring expenses failed for tenant {}: {}", tenant.getCode(), ex.getMessage());
            }
        }
    }
}
