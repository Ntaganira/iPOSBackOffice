--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V10__tenant_constraints_and_admin.sql
-- Date      : 2026. 11. 04.
-- User      : Heritier NTAGANIRA
-- Desc      : Tenant-aware uniqueness, per-tenant settings/sequences and super-admin role.
--

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_username_key;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;
ALTER TABLE units DROP CONSTRAINT IF EXISTS units_name_key;
ALTER TABLE units DROP CONSTRAINT IF EXISTS units_code_key;
ALTER TABLE storage_locations DROP CONSTRAINT IF EXISTS storage_locations_code_key;
ALTER TABLE shop_products DROP CONSTRAINT IF EXISTS shop_products_code_key;
ALTER TABLE shop_products DROP CONSTRAINT IF EXISTS shop_products_barcode_key;
ALTER TABLE shop_customers DROP CONSTRAINT IF EXISTS shop_customers_name_key;
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_name_key;
ALTER TABLE sales DROP CONSTRAINT IF EXISTS sales_receipt_number_key;
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_purchase_number_key;
ALTER TABLE imports DROP CONSTRAINT IF EXISTS imports_reference_no_key;
ALTER TABLE expense_categories DROP CONSTRAINT IF EXISTS expense_categories_name_key;

ALTER TABLE app_users ADD CONSTRAINT uk_app_users_tenant_username UNIQUE (tenant_id, username);
ALTER TABLE categories ADD CONSTRAINT uk_categories_tenant_name UNIQUE (tenant_id, name);
ALTER TABLE units ADD CONSTRAINT uk_units_tenant_name UNIQUE (tenant_id, name);
ALTER TABLE units ADD CONSTRAINT uk_units_tenant_code UNIQUE (tenant_id, code);
ALTER TABLE storage_locations ADD CONSTRAINT uk_storage_locations_tenant_code UNIQUE (tenant_id, code);
ALTER TABLE shop_products ADD CONSTRAINT uk_shop_products_tenant_code UNIQUE (tenant_id, code);
ALTER TABLE shop_products ADD CONSTRAINT uk_shop_products_tenant_barcode UNIQUE (tenant_id, barcode);
ALTER TABLE sales ADD CONSTRAINT uk_sales_tenant_receipt UNIQUE (tenant_id, receipt_number);
ALTER TABLE purchases ADD CONSTRAINT uk_purchases_tenant_number UNIQUE (tenant_id, purchase_number);
ALTER TABLE imports ADD CONSTRAINT uk_imports_tenant_reference UNIQUE (tenant_id, reference_no);
ALTER TABLE expense_categories ADD CONSTRAINT uk_expense_categories_tenant_name UNIQUE (tenant_id, name);

UPDATE app_users
SET email = CASE
    WHEN regexp_replace(username, '[^a-zA-Z0-9._-]', '', 'g') = ''
        THEN 'user' || id
    ELSE lower(regexp_replace(username, '[^a-zA-Z0-9._-]', '', 'g'))
END || '@ipos.local'
WHERE email IS NULL OR btrim(email) = '';

ALTER TABLE app_users ALTER COLUMN email SET NOT NULL;
CREATE UNIQUE INDEX uk_app_users_email_lower ON app_users (lower(email));

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;
ALTER TABLE app_users ADD CONSTRAINT app_users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CASHIER', 'STOREKEEPER', 'VIEWER'));

INSERT INTO roles (code, name, description, system_role)
VALUES ('SUPER_ADMIN', 'Super Administrator', 'Cross-tenant platform administration.', TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE shop_settings_new (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenants(id),
    shop_name VARCHAR(160) NOT NULL DEFAULT 'iPOS Shop',
    shop_tin VARCHAR(40),
    address VARCHAR(255),
    phone VARCHAR(40),
    whatsapp_number VARCHAR(40),
    email VARCHAR(160),
    logo_path VARCHAR(255),
    currency VARCHAR(8) NOT NULL DEFAULT 'RWF',
    receipt_footer VARCHAR(255) DEFAULT 'Thank you for your business!',
    default_print_format VARCHAR(10) NOT NULL DEFAULT 'RECEIPT'
        CHECK (default_print_format IN ('RECEIPT', 'INVOICE')),
    receipt_prefix VARCHAR(10) NOT NULL DEFAULT 'NS-',
    refund_prefix VARCHAR(10) NOT NULL DEFAULT 'RF-',
    allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    auto_print BOOLEAN NOT NULL DEFAULT TRUE,
    expense_approval_threshold NUMERIC(14, 2) NOT NULL DEFAULT 100000.00,
    auto_email_invoice BOOLEAN NOT NULL DEFAULT FALSE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    smtp_host VARCHAR(160),
    smtp_port INTEGER DEFAULT 1025,
    smtp_username VARCHAR(160),
    smtp_password_encrypted VARCHAR(512),
    smtp_security VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (smtp_security IN ('NONE', 'STARTTLS', 'SSL')),
    email_from_name VARCHAR(160),
    email_from_address VARCHAR(160),
    email_reply_to VARCHAR(160),
    email_bcc_shop BOOLEAN NOT NULL DEFAULT FALSE,
    email_subject_template VARCHAR(255) DEFAULT 'Invoice {invoiceNumber} from {shopName}',
    email_body_template TEXT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    theme_primary VARCHAR(7) NOT NULL DEFAULT '#268071',
    theme_secondary VARCHAR(7) NOT NULL DEFAULT '#4FB8A8',
    theme_background VARCHAR(7) NOT NULL DEFAULT '#FFFFFF',
    theme_surface VARCHAR(7) NOT NULL DEFAULT '#FFFFFF'
);

INSERT INTO shop_settings_new (
    tenant_id, shop_name, shop_tin, address, phone, whatsapp_number, email, logo_path,
    currency, receipt_footer, default_print_format, receipt_prefix, refund_prefix,
    allow_negative_stock, auto_print, expense_approval_threshold, auto_email_invoice,
    email_enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted,
    smtp_security, email_from_name, email_from_address, email_reply_to, email_bcc_shop,
    email_subject_template, email_body_template, updated_at, updated_by,
    theme_primary, theme_secondary, theme_background, theme_surface
)
SELECT tenant_id, shop_name, shop_tin, address, phone, whatsapp_number, email, logo_path,
       currency, receipt_footer, default_print_format, receipt_prefix, refund_prefix,
       allow_negative_stock, auto_print, expense_approval_threshold, auto_email_invoice,
       email_enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted,
       smtp_security, email_from_name, email_from_address, email_reply_to, email_bcc_shop,
       email_subject_template, email_body_template, updated_at, updated_by,
       theme_primary, theme_secondary, theme_background, theme_surface
FROM shop_settings;

DROP TABLE shop_settings;
ALTER TABLE shop_settings_new RENAME TO shop_settings;

CREATE TABLE document_sequences_new (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    name VARCHAR(40) NOT NULL,
    value BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_sequences_tenant_name UNIQUE (tenant_id, name)
);

INSERT INTO document_sequences_new (tenant_id, name, value)
SELECT tenant_id, name, value FROM document_sequences;

DROP TABLE document_sequences;
ALTER TABLE document_sequences_new RENAME TO document_sequences;
