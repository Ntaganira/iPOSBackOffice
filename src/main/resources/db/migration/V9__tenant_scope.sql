--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V9__tenant_scope.sql
-- Date      : 2026. 11. 04.
-- User      : Heritier NTAGANIRA
-- Desc      : Adds tenant ownership to every active business table.
--

ALTER TABLE app_users ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE shop_settings ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE categories ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE units ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE storage_locations ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE shop_products ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE shop_customers ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE suppliers ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE sales ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE sale_lines ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE sale_payments ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE document_sequences ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE inventory_movements ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE stock_periods ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE stock_period_lines ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE purchases ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE purchase_lines ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE imports ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE import_items ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE expense_categories ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE expenses ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE customer_payments ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE supplier_payments ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE held_sales ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE audit_logs ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);
ALTER TABLE email_logs ADD COLUMN tenant_id BIGINT REFERENCES tenants(id);

UPDATE app_users SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE shop_settings SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE categories SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE units SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE storage_locations SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE shop_products SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE shop_customers SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE suppliers SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE sales SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE sale_lines SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE sale_payments SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE document_sequences SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE inventory_movements SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE stock_periods SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE stock_period_lines SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE purchases SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE purchase_lines SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE imports SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE import_items SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE expense_categories SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE expenses SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE customer_payments SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE supplier_payments SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE held_sales SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE audit_logs SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE email_logs SET tenant_id = 1 WHERE tenant_id IS NULL;

ALTER TABLE app_users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE shop_settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE units ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE storage_locations ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE shop_products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE shop_customers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sales ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sale_lines ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sale_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE document_sequences ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE inventory_movements ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_periods ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE stock_period_lines ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE purchase_lines ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE imports ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE import_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE expense_categories ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE expenses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE customer_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE supplier_payments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE held_sales ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE email_logs ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX idx_app_users_tenant ON app_users (tenant_id);
CREATE INDEX idx_categories_tenant ON categories (tenant_id);
CREATE INDEX idx_units_tenant ON units (tenant_id);
CREATE INDEX idx_storage_locations_tenant ON storage_locations (tenant_id);
CREATE INDEX idx_shop_products_tenant_active ON shop_products (tenant_id, active);
CREATE INDEX idx_shop_customers_tenant ON shop_customers (tenant_id);
CREATE INDEX idx_suppliers_tenant ON suppliers (tenant_id);
CREATE INDEX idx_sales_tenant_date ON sales (tenant_id, sale_date DESC);
CREATE INDEX idx_sale_lines_tenant ON sale_lines (tenant_id);
CREATE INDEX idx_sale_payments_tenant ON sale_payments (tenant_id);
CREATE INDEX idx_inventory_movements_tenant_occurred ON inventory_movements (tenant_id, occurred_at DESC);
CREATE INDEX idx_stock_periods_tenant ON stock_periods (tenant_id);
CREATE INDEX idx_stock_period_lines_tenant ON stock_period_lines (tenant_id);
CREATE INDEX idx_purchases_tenant_date ON purchases (tenant_id, purchase_date DESC);
CREATE INDEX idx_purchase_lines_tenant ON purchase_lines (tenant_id);
CREATE INDEX idx_imports_tenant_date ON imports (tenant_id, import_date DESC);
CREATE INDEX idx_import_items_tenant ON import_items (tenant_id);
CREATE INDEX idx_expense_categories_tenant ON expense_categories (tenant_id);
CREATE INDEX idx_expenses_tenant_date ON expenses (tenant_id, expense_date DESC);
CREATE INDEX idx_customer_payments_tenant ON customer_payments (tenant_id);
CREATE INDEX idx_supplier_payments_tenant ON supplier_payments (tenant_id);
CREATE INDEX idx_held_sales_tenant ON held_sales (tenant_id);
CREATE INDEX idx_audit_logs_tenant_occurred ON audit_logs (tenant_id, occurred_at DESC);
CREATE INDEX idx_email_logs_tenant_created ON email_logs (tenant_id, created_at DESC);
