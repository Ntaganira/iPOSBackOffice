CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'VIEWER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_cursor BIGINT NOT NULL DEFAULT 0,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    last_push_at TIMESTAMP WITH TIME ZONE,
    last_pull_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sync_inbox (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id),
    outbox_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSED', 'STORED', 'REJECTED')),
    error_text TEXT,
    payload_text TEXT NOT NULL,
    CONSTRAINT uk_sync_inbox_device_outbox UNIQUE (device_id, outbox_id)
);

CREATE INDEX idx_sync_inbox_received ON sync_inbox (received_at DESC);
CREATE INDEX idx_sync_inbox_status ON sync_inbox (status);
CREATE INDEX idx_sync_inbox_entity ON sync_inbox (entity_type, entity_id);

CREATE TABLE invoices (
    id VARCHAR(64) PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id),
    source_outbox_id VARCHAR(64) NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64),
    subtotal_cents BIGINT NOT NULL CHECK (subtotal_cents >= 0),
    tax_cents BIGINT NOT NULL CHECK (tax_cents >= 0),
    core_fee_total_cents BIGINT NOT NULL DEFAULT 0 CHECK (core_fee_total_cents >= 0),
    discount_cents BIGINT NOT NULL DEFAULT 0 CHECK (discount_cents >= 0),
    total_cents BIGINT NOT NULL CHECK (total_cents >= 0),
    tax_rate_bps INTEGER NOT NULL DEFAULT 1800 CHECK (tax_rate_bps >= 0 AND tax_rate_bps <= 10000),
    payment_method VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_invoices_issued ON invoices (issued_at DESC);
CREATE INDEX idx_invoices_device_issued ON invoices (device_id, issued_at DESC);
CREATE INDEX idx_invoices_number ON invoices (invoice_number);
CREATE INDEX idx_invoices_customer ON invoices (customer_id);
CREATE INDEX idx_invoices_payment ON invoices (payment_method);

CREATE TABLE invoice_lines (
    id VARCHAR(64) PRIMARY KEY,
    invoice_id VARCHAR(64) NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id VARCHAR(64),
    sku_snapshot VARCHAR(64) NOT NULL,
    part_name_snapshot VARCHAR(255) NOT NULL,
    brand_snapshot VARCHAR(120),
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    uom_factor INTEGER NOT NULL CHECK (uom_factor > 0),
    uom_label VARCHAR(32) NOT NULL,
    core_fee_cents BIGINT NOT NULL DEFAULT 0 CHECK (core_fee_cents >= 0),
    line_discount_cents BIGINT NOT NULL DEFAULT 0 CHECK (line_discount_cents >= 0),
    tax_rate_bps INTEGER NOT NULL DEFAULT 1800 CHECK (tax_rate_bps >= 0 AND tax_rate_bps <= 10000),
    line_total_cents BIGINT NOT NULL CHECK (line_total_cents >= 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
CREATE INDEX idx_invoice_lines_product ON invoice_lines (product_id);

CREATE TABLE stock_movements (
    id VARCHAR(64) PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id),
    invoice_id VARCHAR(64),
    product_id VARCHAR(64),
    bin_location VARCHAR(32),
    qty_delta INTEGER NOT NULL,
    reference_type VARCHAR(32),
    reference_id VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_movements_invoice ON stock_movements (invoice_id);
CREATE INDEX idx_stock_movements_product ON stock_movements (product_id, occurred_at DESC);
CREATE INDEX idx_stock_movements_occurred ON stock_movements (occurred_at DESC);

CREATE TABLE shift_closures (
    id VARCHAR(64) PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES devices(id),
    closed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    invoice_count INTEGER NOT NULL CHECK (invoice_count >= 0),
    gross_cents BIGINT NOT NULL CHECK (gross_cents >= 0),
    tax_cents BIGINT NOT NULL CHECK (tax_cents >= 0),
    core_fee_cents BIGINT NOT NULL DEFAULT 0 CHECK (core_fee_cents >= 0),
    payment_totals TEXT,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shift_closures_closed ON shift_closures (closed_at DESC);
CREATE INDEX idx_shift_closures_device ON shift_closures (device_id, closed_at DESC);

CREATE TABLE products (
    id VARCHAR(64) PRIMARY KEY,
    sku VARCHAR(64),
    oem_number VARCHAR(120),
    part_name VARCHAR(255),
    brand VARCHAR(120),
    retail_price_cents BIGINT NOT NULL DEFAULT 0 CHECK (retail_price_cents >= 0),
    trade_price_cents BIGINT NOT NULL DEFAULT 0 CHECK (trade_price_cents >= 0),
    core_fee_cents BIGINT NOT NULL DEFAULT 0 CHECK (core_fee_cents >= 0),
    base_unit VARCHAR(16),
    updated_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_device_id BIGINT REFERENCES devices(id)
);

CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_updated ON products (updated_at DESC);

CREATE TABLE customers (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(200),
    phone VARCHAR(40),
    tax_id VARCHAR(40),
    balance_cents BIGINT NOT NULL DEFAULT 0 CHECK (balance_cents >= 0),
    updated_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_device_id BIGINT REFERENCES devices(id)
);

CREATE INDEX idx_customers_name ON customers (display_name);
CREATE INDEX idx_customers_updated ON customers (updated_at DESC);

CREATE TABLE uom_conversions (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    from_unit VARCHAR(32) NOT NULL,
    to_unit VARCHAR(32) NOT NULL,
    factor INTEGER NOT NULL CHECK (factor > 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_device_id BIGINT REFERENCES devices(id)
);

CREATE INDEX idx_uom_conversions_product ON uom_conversions (product_id);
