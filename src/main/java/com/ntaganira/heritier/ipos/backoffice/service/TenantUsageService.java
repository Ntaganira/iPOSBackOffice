package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : TenantUsageService.java
 * - Date      : 2026. 11. 04.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Reports per-tenant row counts for the platform dashboard.
 * </pre>
 */

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantUsageService {

    private static final List<TableDefinition> TABLES = List.of(
            new TableDefinition("app_users", "Users"),
            new TableDefinition("shop_settings", "Settings"),
            new TableDefinition("categories", "Categories"),
            new TableDefinition("units", "Units"),
            new TableDefinition("storage_locations", "Locations"),
            new TableDefinition("shop_products", "Products"),
            new TableDefinition("shop_customers", "Customers"),
            new TableDefinition("suppliers", "Suppliers"),
            new TableDefinition("sales", "Sales"),
            new TableDefinition("sale_lines", "Sale lines"),
            new TableDefinition("sale_payments", "Sale payments"),
            new TableDefinition("document_sequences", "Sequences"),
            new TableDefinition("inventory_movements", "Stock movements"),
            new TableDefinition("stock_periods", "Stock periods"),
            new TableDefinition("stock_period_lines", "Stock period lines"),
            new TableDefinition("purchases", "Purchases"),
            new TableDefinition("purchase_lines", "Purchase lines"),
            new TableDefinition("imports", "Imports"),
            new TableDefinition("import_items", "Import items"),
            new TableDefinition("expense_categories", "Expense categories"),
            new TableDefinition("expenses", "Expenses"),
            new TableDefinition("customer_payments", "Customer payments"),
            new TableDefinition("supplier_payments", "Supplier payments"),
            new TableDefinition("held_sales", "Held sales"),
            new TableDefinition("audit_logs", "Audit entries"),
            new TableDefinition("email_logs", "Email entries")
    );

    private final JdbcTemplate jdbcTemplate;

    public TenantUsageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Long> counts(long tenantId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        TABLES.forEach(table -> counts.put(table.table(), count(table.table(), tenantId)));
        return counts;
    }

    public long count(String table, long tenantId) {
        Long value = jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where tenant_id = ?", Long.class, tenantId);
        return value == null ? 0 : value;
    }

    public long totalTenants() {
        return value("select count(*) from tenants where id <> 0");
    }

    public long activeTenants() {
        return value("select count(*) from tenants where id <> 0 and is_active = true");
    }

    public long totalUsers() {
        return value("select count(*) from app_users where tenant_id <> 0");
    }

    public long totalSales() {
        return value("select count(*) from sales");
    }

    private long value(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    public record TableCount(String table, String label, long rows) {
    }

    private record TableDefinition(String table, String label) {
    }
}
