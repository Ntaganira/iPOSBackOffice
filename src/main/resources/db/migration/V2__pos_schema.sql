-- iPOS point of sale, stock, purchasing, expenses, RBAC, settings and audit schema.

-- ---------------------------------------------------------------------------
-- Users and access control
-- ---------------------------------------------------------------------------
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;
ALTER TABLE app_users ADD CONSTRAINT app_users_role_check
    CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER', 'STOREKEEPER', 'VIEWER'));
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone VARCHAR(40);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(100);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS locale VARCHAR(10) DEFAULT 'en';

CREATE TABLE roles (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    code VARCHAR(60) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE role_permissions (
    role_code VARCHAR(30) NOT NULL REFERENCES roles(code) ON DELETE CASCADE,
    permission_code VARCHAR(60) NOT NULL REFERENCES permissions(code) ON DELETE CASCADE,
    PRIMARY KEY (role_code, permission_code)
);

CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_code);

-- ---------------------------------------------------------------------------
-- Shop settings and tax categories
-- ---------------------------------------------------------------------------
CREATE TABLE shop_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    shop_name VARCHAR(160) NOT NULL DEFAULT 'iPOS Shop',
    shop_tin VARCHAR(40),
    address VARCHAR(255),
    phone VARCHAR(40),
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
    updated_by VARCHAR(50)
);

CREATE TABLE tax_categories (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    exempt BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- Catalogue, customers and suppliers
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE TABLE units (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE TABLE shop_products (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    barcode VARCHAR(80) UNIQUE,
    name VARCHAR(200) NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    unit_id BIGINT REFERENCES units(id),
    buying_price NUMERIC(14, 2) NOT NULL DEFAULT 0,
    selling_price NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_category_code VARCHAR(2) REFERENCES tax_categories(code),
    reorder_level NUMERIC(14, 3) NOT NULL DEFAULT 0,
    stock_quantity NUMERIC(14, 3) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image_path VARCHAR(255),
    image_content_type VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_shop_products_name ON shop_products (LOWER(name));
CREATE INDEX idx_shop_products_barcode ON shop_products (barcode);
CREATE INDEX idx_shop_products_active ON shop_products (active);

CREATE TABLE shop_customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    tin VARCHAR(40),
    phone VARCHAR(40),
    email VARCHAR(160),
    address VARCHAR(255),
    customer_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL'
        CHECK (customer_type IN ('INDIVIDUAL', 'COMPANY')),
    credit_limit NUMERIC(14, 2) NOT NULL DEFAULT 0,
    balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_shop_customers_name ON shop_customers (LOWER(name));
CREATE INDEX idx_shop_customers_phone ON shop_customers (phone);

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    tin VARCHAR(40),
    phone VARCHAR(40),
    email VARCHAR(160),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

-- ---------------------------------------------------------------------------
-- Sales
-- ---------------------------------------------------------------------------
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(40) NOT NULL UNIQUE,
    sale_type VARCHAR(10) NOT NULL DEFAULT 'SALE' CHECK (sale_type IN ('SALE', 'REFUND')),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED', 'REFUNDED', 'VOID')),
    refund_of_sale_id BIGINT REFERENCES sales(id),
    customer_id BIGINT REFERENCES shop_customers(id),
    cashier_id BIGINT REFERENCES app_users(id),
    sale_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    amount_paid NUMERIC(14, 2) NOT NULL DEFAULT 0,
    change_due NUMERIC(14, 2) NOT NULL DEFAULT 0,
    balance_due NUMERIC(14, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID'
        CHECK (payment_status IN ('PAID', 'PARTIAL', 'UNPAID')),
    notes VARCHAR(500),
    ebm_signature VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_sales_date ON sales (sale_date DESC);
CREATE INDEX idx_sales_customer ON sales (customer_id);
CREATE INDEX idx_sales_cashier ON sales (cashier_id);
CREATE INDEX idx_sales_status ON sales (status);

CREATE TABLE sale_lines (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES shop_products(id),
    product_name VARCHAR(200) NOT NULL,
    product_code VARCHAR(60),
    quantity NUMERIC(14, 3) NOT NULL,
    unit_price NUMERIC(14, 2) NOT NULL,
    unit_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_category_code VARCHAR(2),
    tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14, 2) NOT NULL,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_sale_lines_sale ON sale_lines (sale_id);
CREATE INDEX idx_sale_lines_product ON sale_lines (product_id);

CREATE TABLE sale_payments (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'BANK', 'CREDIT')),
    amount NUMERIC(14, 2) NOT NULL,
    reference VARCHAR(80)
);

CREATE INDEX idx_sale_payments_sale ON sale_payments (sale_id);

CREATE TABLE document_sequences (
    name VARCHAR(40) PRIMARY KEY,
    value BIGINT NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- Inventory ledger
-- ---------------------------------------------------------------------------
CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES shop_products(id),
    movement_type VARCHAR(20) NOT NULL
        CHECK (movement_type IN ('OPENING', 'PURCHASE', 'IMPORT', 'SALE', 'REFUND', 'ADJUSTMENT', 'RECEPTION')),
    quantity_delta NUMERIC(14, 3) NOT NULL,
    unit_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    reference_type VARCHAR(30),
    reference_id VARCHAR(60),
    note VARCHAR(255),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

CREATE INDEX idx_inventory_movements_product ON inventory_movements (product_id, occurred_at DESC);
CREATE INDEX idx_inventory_movements_occurred ON inventory_movements (occurred_at DESC);
CREATE INDEX idx_inventory_movements_type ON inventory_movements (movement_type);

CREATE TABLE stock_periods (
    id BIGSERIAL PRIMARY KEY,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    opened_by VARCHAR(50),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_by VARCHAR(50),
    closed_at TIMESTAMP WITH TIME ZONE,
    notes VARCHAR(255)
);

CREATE TABLE stock_period_lines (
    id BIGSERIAL PRIMARY KEY,
    period_id BIGINT NOT NULL REFERENCES stock_periods(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES shop_products(id),
    opening_qty NUMERIC(14, 3) NOT NULL DEFAULT 0,
    opening_value NUMERIC(14, 2) NOT NULL DEFAULT 0,
    closing_qty NUMERIC(14, 3) NOT NULL DEFAULT 0,
    closing_value NUMERIC(14, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_stock_period_lines_period ON stock_period_lines (period_id);

-- ---------------------------------------------------------------------------
-- Purchases and imports
-- ---------------------------------------------------------------------------
CREATE TABLE purchases (
    id BIGSERIAL PRIMARY KEY,
    purchase_number VARCHAR(40) NOT NULL UNIQUE,
    supplier_id BIGINT REFERENCES suppliers(id),
    purchase_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ORDERED'
        CHECK (status IN ('ORDERED', 'PARTIAL', 'RECEIVED', 'CANCELLED')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
        CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID')),
    subtotal NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    amount_paid NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes VARCHAR(500),
    received_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE TABLE purchase_lines (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES shop_products(id),
    quantity_ordered NUMERIC(14, 3) NOT NULL,
    quantity_received NUMERIC(14, 3) NOT NULL DEFAULT 0,
    unit_cost NUMERIC(14, 2) NOT NULL,
    line_total NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_purchase_lines_purchase ON purchase_lines (purchase_id);

CREATE TABLE imports (
    id BIGSERIAL PRIMARY KEY,
    reference_no VARCHAR(60) NOT NULL UNIQUE,
    supplier_id BIGINT REFERENCES suppliers(id),
    origin_country VARCHAR(80),
    currency VARCHAR(8) NOT NULL DEFAULT 'USD',
    exchange_rate NUMERIC(14, 6) NOT NULL DEFAULT 1,
    customs_duty NUMERIC(14, 2) NOT NULL DEFAULT 0,
    taxes NUMERIC(14, 2) NOT NULL DEFAULT 0,
    freight NUMERIC(14, 2) NOT NULL DEFAULT 0,
    other_costs NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_landed NUMERIC(14, 2) NOT NULL DEFAULT 0,
    import_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'RECEIVED', 'CANCELLED')),
    notes VARCHAR(500),
    received_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE TABLE import_items (
    id BIGSERIAL PRIMARY KEY,
    import_id BIGINT NOT NULL REFERENCES imports(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES shop_products(id),
    quantity NUMERIC(14, 3) NOT NULL,
    unit_cost_foreign NUMERIC(14, 2) NOT NULL,
    landed_unit_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_import_items_import ON import_items (import_id);

-- ---------------------------------------------------------------------------
-- Expenses
-- ---------------------------------------------------------------------------
CREATE TABLE expense_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    expense_date DATE NOT NULL,
    category_id BIGINT NOT NULL REFERENCES expense_categories(id),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    tax_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'BANK')),
    reference VARCHAR(80),
    payee VARCHAR(160),
    attachment_path VARCHAR(255),
    attachment_content_type VARCHAR(100),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PAID'
        CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'REJECTED', 'VOID')),
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence VARCHAR(20) CHECK (recurrence IN ('WEEKLY', 'MONTHLY')),
    next_occurrence DATE,
    approved_by VARCHAR(50),
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(255),
    void_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_expenses_date ON expenses (expense_date DESC);
CREATE INDEX idx_expenses_status ON expenses (status);
CREATE INDEX idx_expenses_category ON expenses (category_id);

-- ---------------------------------------------------------------------------
-- Customer / supplier payments
-- ---------------------------------------------------------------------------
CREATE TABLE customer_payments (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES shop_customers(id),
    sale_id BIGINT REFERENCES sales(id),
    amount NUMERIC(14, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'BANK')),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(80),
    created_by VARCHAR(50)
);

CREATE INDEX idx_customer_payments_customer ON customer_payments (customer_id, paid_at DESC);

CREATE TABLE supplier_payments (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    purchase_id BIGINT REFERENCES purchases(id),
    amount NUMERIC(14, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'CARD', 'BANK')),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(80),
    created_by VARCHAR(50)
);

CREATE INDEX idx_supplier_payments_supplier ON supplier_payments (supplier_id, paid_at DESC);

-- ---------------------------------------------------------------------------
-- Held (parked) POS sales
-- ---------------------------------------------------------------------------
CREATE TABLE held_sales (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(120) NOT NULL,
    cashier_id BIGINT REFERENCES app_users(id),
    customer_id BIGINT REFERENCES shop_customers(id),
    cart_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Audit and email logs
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    username VARCHAR(50),
    role VARCHAR(30),
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(60),
    entity_label VARCHAR(255),
    changes_json TEXT,
    ip_address VARCHAR(60),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs (username);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);

CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    recipient VARCHAR(255) NOT NULL,
    cc VARCHAR(255),
    subject VARCHAR(255),
    document_type VARCHAR(30),
    document_number VARCHAR(60),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_email_logs_created ON email_logs (created_at DESC);
CREATE INDEX idx_email_logs_status ON email_logs (status);
